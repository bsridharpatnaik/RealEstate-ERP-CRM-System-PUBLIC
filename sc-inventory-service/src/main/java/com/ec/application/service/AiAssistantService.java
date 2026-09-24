package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.AiChatLog;
import com.ec.application.repository.AiChatLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Admin AI assistant: runs the Claude CLI via an external command (prod: sudo wrapper that runs it as the
 * sandboxed `erpai` user inside a resource-capped slice). Contract: arg = session id or "-",
 * question on stdin, Claude's JSON result on stdout.
 */
@Service
@UseDefaultTenant
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);
    private static final Pattern SESSION_ID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final int MAX_QUESTION_CHARS = 2000;

    // ponytail: per-JVM limit — fine for a single Tomcat; needs a shared lock if the service is ever scaled out.
    private final Semaphore slots = new Semaphore(2);

    @Value("${ai.assistant.command:sudo -n /usr/local/bin/erp-ai-ask}")
    private String command;

    @Value("${ai.assistant.timeout-seconds:180}")
    private long timeoutSeconds;

    @Autowired
    private AiChatLogRepository aiChatLogRepository;

    @Autowired
    private AiUsageService aiUsageService;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> ask(String username, String question, String sessionId) throws Exception {
        question = question == null ? "" : question.trim();
        if (question.isEmpty() || question.length() > MAX_QUESTION_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must be 1–" + MAX_QUESTION_CHARS + " characters");
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            // Only the admin who started a conversation may continue it.
            if (!SESSION_ID.matcher(sessionId).matches()
                    || !aiChatLogRepository.existsByUsernameAndSessionId(username, sessionId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown conversation — start a new chat");
            }
        } else {
            sessionId = null;
        }
        aiUsageService.checkLimit(username);
        if (!slots.tryAcquire(20, TimeUnit.SECONDS)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "The assistant is busy. Please try again in a minute.");
        }

        AiChatLog entry = new AiChatLog();
        entry.setUsername(username);
        entry.setQuestion(question);
        entry.setSessionId(sessionId);
        entry.setCreatedAt(new Date());
        entry.setStatus("ERROR");
        long start = System.currentTimeMillis();
        try {
            JsonNode result = run(question, sessionId);
            entry.setAnswer(result.path("result").asText(""));
            entry.setCostUsd(result.path("total_cost_usd").asDouble(0));
            entry.setNumTurns(result.path("num_turns").asInt(0));
            if (result.hasNonNull("session_id")) {
                entry.setSessionId(result.get("session_id").asText());
            }
            if (!result.path("is_error").asBoolean(false)) {
                entry.setStatus("OK");
            } else if (entry.getAnswer().isEmpty()) {
                entry.setAnswer("Sorry, I couldn't complete that. Please rephrase or narrow the question.");
            }
        } catch (ResponseStatusException e) {
            if (e.getStatus() == HttpStatus.GATEWAY_TIMEOUT) {
                entry.setStatus("TIMEOUT");
            }
            entry.setAnswer(e.getReason());
            throw e;
        } finally {
            slots.release();
            entry.setDurationMs(System.currentTimeMillis() - start);
            aiChatLogRepository.save(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>(aiUsageService.usage(username));
        response.put("answer", entry.getAnswer());
        response.put("sessionId", entry.getSessionId());
        response.put("costUsd", entry.getCostUsd());
        return response;
    }

    private JsonNode run(String question, String sessionId) throws Exception {
        List<String> cmd = new ArrayList<>(Arrays.asList(command.trim().split("\\s+")));
        cmd.add(sessionId == null ? "-" : sessionId);

        File out = File.createTempFile("ai-out", ".json");
        File err = File.createTempFile("ai-err", ".log");
        try {
            Process p = new ProcessBuilder(cmd).redirectOutput(out).redirectError(err).start();
            try (OutputStream stdin = p.getOutputStream()) {
                stdin.write(question.getBytes(StandardCharsets.UTF_8));
            }
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "The assistant took too long. Try a narrower question.");
            }
            String stdout = new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8);
            int json = stdout.indexOf('{');
            if (json < 0) {
                log.error("AI assistant failed (exit {}): {}", p.exitValue(),
                        new String(Files.readAllBytes(err.toPath()), StandardCharsets.UTF_8));
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The assistant is unavailable right now.");
            }
            return objectMapper.readTree(stdout.substring(json));
        } finally {
            out.delete();
            err.delete();
        }
    }
}
