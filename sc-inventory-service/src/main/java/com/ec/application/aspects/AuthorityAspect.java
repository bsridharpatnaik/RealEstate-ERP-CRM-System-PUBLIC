package com.ec.application.aspects;

import com.ec.application.service.AuthorizationService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuthorityAspect {

    @Autowired
    AuthorizationService authorizationService;

    @Before("@annotation(CheckAuthority)")
    public void checkAuthority() throws Throwable {
        authorizationService.exitIfReadOnly();
    }
}
