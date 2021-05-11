package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.model.APICallTypeForAuthorization;
import com.ec.application.model.TransactionTypeForAuthorization;

@Service
public class AuthorizationService {
    @Autowired
    UserDetailsService userService;

    public void exitIfNOtAuthorizedCreate(TransactionTypeForAuthorization type, APICallTypeForAuthorization userAction) {

    }

    public void exitIfNOtAuthorizedUpdate() {

    }

    public void exitIfNOtAuthorizedDelete() {

    }
}
