package com.ec.application.service;

import com.ec.application.data.AuthorizationEnum;
import com.ec.application.data.UserReturnData;
import com.ec.application.data.UserTenantMapping;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ec.application.model.APICallTypeForAuthorization;
import com.ec.application.model.TransactionTypeForAuthorization;

@Service
public class AuthorizationService {

    @Autowired
    UserDetailsService userService;

    @Value("${spring.profiles.active}")
    private String profile;

    public void exitIfReadOnly() throws Exception {
        UserReturnData currentUser = userService.getCurrentUser();
        String tenantName = ThreadLocalStorage.getTenantName();
        boolean isAllowed = false;

        for (UserTenantMapping ut : currentUser.getTenantList()) {

            if (profile.contains("sc") && profile.contains("new")) {
                tenantName = tenantName.replace("new", "");
            }
            if (ut.getTenant().getName().equalsIgnoreCase(tenantName) && ut.getAuthorization().equals(AuthorizationEnum.FullAccess)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed)
            throw new Exception("User not allowed to add/modify data for this project");
    }

}
