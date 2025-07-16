package com.adstartmedia.domainwatchdog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WebApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(WebApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException e) {
        return e.getMessage();
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<String> generalException(Exception e) {
        log.atError().setCause(e).log("Unexpected exception!");
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}