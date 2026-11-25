package com.sysc4806.gradadmissions.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
public class MultipartConfig {
    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilter() {
        MultipartFilter filter = new MultipartFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                try {
                    super.doFilterInternal(request, response, chain);
                } catch (MaxUploadSizeExceededException e) {
                    String message = "Choose a PDF no larger than 5 MB.";
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.setContentLength(message.length());
                    response.getWriter().write(message);
                }
            }
        };
        filter.setMultipartResolverBeanName("multipartResolver");
        FilterRegistrationBean<MultipartFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/apply/*");
        // Parse the hidden CSRF token before security, and report size failures before token validation.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
