package com.ec.application.aspects;

import com.ec.application.data.UserReturnData;
import com.ec.application.service.UserDetailsService;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class AllowOnlyAspect {

    @Autowired
    private UserDetailsService userDetailsService;

    @Around("@annotation(com.ec.application.aspects.AllowOnly) || " +
            "@within(com.ec.application.aspects.AllowOnly)")
    public Object enforceAuthorization(ProceedingJoinPoint joinPoint) throws Throwable {

        AllowOnly allowOnly = resolveAnnotation(joinPoint);
        if (allowOnly == null) {
            return joinPoint.proceed();
        }

        UserReturnData user = userDetailsService.getCurrentUser();
        if (user == null || user.getRoles() == null) {
            throw new RuntimeException("Unauthenticated user");
        }

        // Normalize roles
        Set<String> userRoles = user.getRoles()
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Check allowed roles
        for (String allowed : allowOnly.roles()) {
            if (userRoles.contains(allowed.toLowerCase())) {
                return joinPoint.proceed();
            }
        }

        throw new RuntimeException(
                "Access denied. Required role(s): " + String.join(", ", allowOnly.roles())
        );
    }

    private AllowOnly resolveAnnotation(ProceedingJoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Method-level first
        AllowOnly methodAnno = method.getAnnotation(AllowOnly.class);
        if (methodAnno != null) {
            return methodAnno;
        }

        // Class-level
        return joinPoint.getTarget()
                .getClass()
                .getAnnotation(AllowOnly.class);
    }
}