package com.example.demo.config;

import java.util.Collections;

import jakarta.servlet.SessionTrackingMode;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfig {

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
        	// セッション管理をCookieのみを使うよう設定
            servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));
        };
    }
}
