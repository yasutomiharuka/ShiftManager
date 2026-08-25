package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.example.demo.service.UserProfileDetailsService;

@Configuration
public class SecurityConfig {

    private final UserProfileDetailsService userDetailsService;

    public SecurityConfig(UserProfileDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // セッション管理の設定
            .sessionManagement(session -> session
                .maximumSessions(1)
                // 上限を超えた場合、既存セッションを無効化
                .maxSessionsPreventsLogin(false)
                .and()
                // セッションフィクセーション攻撃を防止
                .sessionFixation().migrateSession()
            )

            // CSRF保護の設定
            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    new HttpSessionCsrfTokenRepository()
                )
            )

            // 認可ルールの設定
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/register",
                    "/login",
                    "/user/register/**",
                    "/error/**",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // 権限不足（403）の場合の遷移先
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
            )

            // フォームログインの設定
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
                .permitAll()
            )

            // ログアウトの設定
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .permitAll()
            );

        return http.build();
    }

    // パスワードエンコーダーの設定
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 認証プロバイダーの設定
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    // 認証マネージャーの設定
    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            PasswordEncoder passwordEncoder) throws Exception {

        return http.getSharedObject(AuthenticationManager.class);
    }

    // 並行セッションを管理するためのイベント発行
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}