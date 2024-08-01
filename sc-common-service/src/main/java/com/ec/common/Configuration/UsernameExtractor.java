package com.ec.common.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class UsernameExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractUsernameFromLoginPayload(HttpServletRequest request) {
        try {
            JsonNode jsonNode = objectMapper.readTree(request.getInputStream());
            return jsonNode.path("userName").asText();
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception properly
            return "";
        }
    }
}
