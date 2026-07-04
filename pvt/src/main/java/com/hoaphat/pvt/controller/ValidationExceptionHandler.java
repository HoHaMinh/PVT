package com.hoaphat.pvt.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

@ControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler({
            BindException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public Object handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return buildResponse("Dữ liệu gửi lên bị thiếu hoặc không đúng định dạng.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrity(DataIntegrityViolationException exception,
                                      HttpServletRequest request) {
        return buildResponse("Không thể lưu vì dữ liệu không còn phù hợp với database. Vui lòng tải lại trang và thử lại.", request);
    }

    private Object buildResponse(String message, HttpServletRequest request) {
        boolean ajaxRequest = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null
                && request.getHeader("Accept").contains("application/json"));
        if (ajaxRequest) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", message));
        }

        ModelAndView modelAndView = new ModelAndView("validation-error");
        modelAndView.setStatus(HttpStatus.BAD_REQUEST);
        modelAndView.addObject("message", message);
        return modelAndView;
    }
}
