package com.ec.application.service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.RoleConstants;
import com.ec.application.data.UserTenantMapping;
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
public class UserDetailsService {

    @Autowired
    WebClient.Builder webClientBuilder;

    @Value("${common.serverurl}")
    private String reqUrl;

    @Autowired
    SchemaConfig schemaConfig;

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

    /** Admin or Purchase Manager — full write access to PO, Indents, Global Config. */
    public boolean isAdminOrPurchaseManager() throws Exception {
        return (hasRole(RoleConstants.ADMIN) || hasRole(RoleConstants.PURCHASE_MANAGER));
    }

    /** Roles that can edit Indent records (NEW or APPROVED, before any PO is created).
     *  Admin, Purchase Manager, and Project Manager — excludes Store Incharge for APPROVED edits. */
    public boolean canEditIndentAsManager() throws Exception {
        return (hasRole(RoleConstants.ADMIN)
                || hasRole(RoleConstants.PURCHASE_MANAGER)
                || hasRole(RoleConstants.PROJECT_MANAGER));
    }

    /** Roles that can approve, reject, or cancel Indents. */
    public boolean canApproveRejectCancelIndent() throws Exception {
        return (hasRole(RoleConstants.ADMIN)
                || hasRole(RoleConstants.PURCHASE_MANAGER)
                || hasRole(RoleConstants.PROJECT_MANAGER));
    }

    /** Store Incharge — can create/edit own project indents and cancel NEW indents. */
    public boolean isStoreIncharge() throws Exception {
        return hasRole(RoleConstants.STORE_INCHARGE)
                && !hasRole(RoleConstants.ADMIN)
                && !hasRole(RoleConstants.PURCHASE_MANAGER);
    }

    /** Roles that must NOT see pricing/rate data anywhere in the system. */
    public boolean isPriceRestricted() throws Exception {
        return (hasRole(RoleConstants.PROJECT_MANAGER) || hasRole(RoleConstants.STORE_INCHARGE))
                && !hasRole(RoleConstants.ADMIN)
                && !hasRole(RoleConstants.PURCHASE_MANAGER)
                && !hasRole(RoleConstants.MANAGEMENT);
    }
}
