package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.model.AiChatLog;
import com.ec.application.model.AiUsageLimit;
import com.ec.application.repository.AiChatLogRepository;
import com.ec.application.repository.AiUsageLimitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** Per-user daily AI chat limit, plus the admin usage report and history. */
@Service
@UseDefaultTenant
public class AiUsageService {

    private static final int FALLBACK_DAILY_REQUESTS = 5;   // until an admin saves a default on the AI Usage page

    @Autowired private AiChatLogRepository aiChatLogRepository;
    @Autowired private AiUsageLimitRepository aiUsageLimitRepository;

    private static final int HISTORY_MAX_ROWS = 500;

    public long usedToday(String username) {
        return aiChatLogRepository.countByUsernameAndCreatedAtGreaterThanEqual(username, daysAgo(0));
    }

    public int limitFor(String username) {
        AiUsageLimit own = aiUsageLimitRepository.findByUsername(username.toLowerCase());
        return own != null ? own.getDailyRequests() : defaultLimit();
    }

    // ponytail: count-then-call, so two simultaneous requests at N-1 can both pass; fine for a soft daily limit.
    public void checkLimit(String username) {
        long used = usedToday(username);
        int limit = limitFor(username);
        if (used >= limit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily AI limit reached (" + used + "/" + limit + " requests). It resets at midnight.");
        }
    }

    public Map<String, Object> usage(String username) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("usedToday", usedToday(username));
        m.put("dailyLimit", limitFor(username));
        return m;
    }

    /** Admin page: today's questions, limit, and last-30-day questions + cost per user. */
    public Map<String, Object> report() {
        Map<String, Map<String, Object>> users = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Object[] r : aiChatLogRepository.usageByUserSince(daysAgo(0))) add(users, (String) r[0], "chatToday", r[1], null);
        for (Object[] r : aiChatLogRepository.usageByUserSince(daysAgo(29))) add(users, (String) r[0], "requests30d", r[1], r[2]);

        Map<String, Integer> overrides = new HashMap<>();
        for (AiUsageLimit l : aiUsageLimitRepository.findAll()) {
            if (!AiUsageLimit.DEFAULT_USER.equals(l.getUsername())) {
                overrides.put(l.getUsername(), l.getDailyRequests());
                users.computeIfAbsent(l.getUsername(), this::emptyRow);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        users.forEach((name, row) -> {
            row.put("dailyLimit", overrides.get(name.toLowerCase()));   // null = uses default
            rows.add(row);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("defaultDailyLimit", defaultLimit());
        result.put("users", rows);
        return result;
    }

    /** Admin audit: who asked what in the chat, newest first. username null = everyone. */
    // ponytail: newest 500 rows; add paging if admins need to scroll further back.
    public List<Map<String, Object>> history(String username, int days) {
        String user = username == null || username.trim().isEmpty() ? null : username.trim();
        Date since = daysAgo(Math.max(0, days - 1));
        PageRequest page = PageRequest.of(0, HISTORY_MAX_ROWS);
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AiChatLog c : aiChatLogRepository.history(user, since, page)) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("time", fmt.format(c.getCreatedAt()));
            r.put("username", c.getUsername());
            r.put("text", c.getQuestion());
            r.put("status", c.getStatus());
            r.put("answer", c.getAnswer());
            r.put("costUsd", c.getCostUsd() == null ? 0.0 : c.getCostUsd());
            rows.add(r);
        }
        return rows;
    }

    /** dailyRequests null removes a user's override (falls back to the default). */
    public void setLimit(String username, Integer dailyRequests) {
        username = username == null ? "" : username.trim().toLowerCase();
        if (username.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        if (dailyRequests != null && dailyRequests < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit cannot be negative");
        if (dailyRequests == null && AiUsageLimit.DEFAULT_USER.equals(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default limit is required");
        }
        AiUsageLimit row = aiUsageLimitRepository.findByUsername(username);
        if (dailyRequests == null) {
            if (row != null) aiUsageLimitRepository.delete(row);
            return;
        }
        if (row == null) {
            row = new AiUsageLimit();
            row.setUsername(username);
        }
        row.setDailyRequests(dailyRequests);
        aiUsageLimitRepository.save(row);
    }

    private int defaultLimit() {
        AiUsageLimit d = aiUsageLimitRepository.findByUsername(AiUsageLimit.DEFAULT_USER);
        return d != null ? d.getDailyRequests() : FALLBACK_DAILY_REQUESTS;
    }

    private void add(Map<String, Map<String, Object>> users, String username, String key, Object count, Object cost) {
        Map<String, Object> row = users.computeIfAbsent(username, this::emptyRow);
        row.put(key, ((Number) row.get(key)).longValue() + ((Number) count).longValue());
        if (cost != null) row.put("cost30dUsd", ((Number) row.get("cost30dUsd")).doubleValue() + ((Number) cost).doubleValue());
    }

    private Map<String, Object> emptyRow(String username) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("username", username);
        row.put("chatToday", 0L);
        row.put("requests30d", 0L);
        row.put("cost30dUsd", 0.0);
        return row;
    }

    private static Date daysAgo(int days) {
        return Date.from(LocalDate.now().minusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
