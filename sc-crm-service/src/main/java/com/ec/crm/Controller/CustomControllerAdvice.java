package com.ec.crm.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class CustomControllerAdvice {
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Bad Request - Please check your inputs.")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void handleException() {
        //Handle Exception Here...
    }
}
