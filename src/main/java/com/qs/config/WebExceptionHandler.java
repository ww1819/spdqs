package com.qs.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        if (!isIgnoredResource(request.getRequestURI())) {
            log.debug("资源不存在: {}", request.getRequestURI());
        }
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        log.warn("请求 {} 参数错误: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(apiError(ex.getMessage()));
        }
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneral(Exception ex, Model model, HttpServletRequest request) {
        log.error("请求 {} 发生异常", request.getRequestURI(), ex);
        if (isApiRequest(request)) {
            String msg = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "系统处理请求时发生错误";
            return ResponseEntity.internalServerError().body(apiError(msg));
        }
        model.addAttribute("message", "系统处理请求时发生错误，请稍后重试或联系管理员。");
        return "error";
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/");
    }

    private java.util.Map<String, String> apiError(String message) {
        return java.util.Map.of("error", message != null ? message : "请求失败");
    }

    private boolean isIgnoredResource(String uri) {
        return uri != null && (uri.endsWith("/favicon.ico") || uri.endsWith("/favicon.svg"));
    }
}
