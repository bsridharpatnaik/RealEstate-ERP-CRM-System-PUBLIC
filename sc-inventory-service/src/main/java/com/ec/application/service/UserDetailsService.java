package com.ec.application.service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import com.ec.application.constants.RoleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import com.ec.application.data.UserReturnData;

import java.util.Collections;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;

@Service
@Transactional
public class UserDetailsService {

    @Autowired
    WebClient.Builder webClientBuilder;

    @Value("${common.serverurl}")
    private String reqUrl;

    Logger log = LoggerFactory.getLogger(UserDetailsService.class);

    @Cacheable(
            value = "currentUser",
            key = "#root.methodName + ':' + T(com.ec.application.util.AuthUtil).getAuthKey()",
            unless = "#result == null"
    )
    public UserReturnData getCurrentUser() throws Exception {

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) {
            return systemUser();
        }

        HttpServletRequest request = attrs.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null) {
            return systemUser();
        }

        try {
            log.debug("Fetching user from auth service");

            return webClientBuilder.build()
                    .get()
                    .uri(reqUrl + "user/me")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .bodyToMono(UserReturnData.class)
                    .block();

        } catch (Exception e) {
            throw new Exception(
                    "Unable to fetch current user. Please contact system administrator."
            );
        }
    }

    private UserReturnData systemUser() {
        UserReturnData user = new UserReturnData();
        user.setId(404L);
        user.setUsername("system");
        user.setRoles(Collections.singletonList("SYSTEM"));
        return user;
    }

    // 👇 ALL THESE NOW USE CACHE AUTOMATICALLY

    public String getCurrentUserRole() throws Exception {
        UserReturnData user = getCurrentUser();
        return (user.getRoles() == null || user.getRoles().isEmpty())
                ? "SYSTEM"
                : user.getRoles().get(0);
    }

    public List<String> getCurrentUserRoles() throws Exception {
        UserReturnData user = getCurrentUser();
        return user.getRoles() == null
                ? Collections.singletonList("SYSTEM")
                : user.getRoles();
    }

    public boolean hasRole(String role) throws Exception {
        return getCurrentUserRoles().stream()
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }

    public boolean isAdminOrManager() throws Exception {
        return (hasRole(RoleConstants.ADMIN) || hasRole(RoleConstants.INVENTORY_MANAGER));
    }

    public boolean isInventoryExecutive() throws Exception {
        return (hasRole(RoleConstants.INVENTORY_EXECUTIVE) && !hasRole(RoleConstants.INVENTORY_MANAGER) && !hasRole(RoleConstants.ADMIN));
    }
}
