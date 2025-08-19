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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // セッション管理の設定
            .sessionManagement(session -> session
                .maximumSessions(1) // 同時ログインセッション数を制限
                .maxSessionsPreventsLogin(false) // セッション数を超えた場合、既存のセッションを無効化
                .and()
                .sessionFixation().migrateSession() // セッションフィクセーション攻撃を防止
            )
            // CSRF保護の設定
            .csrf(csrf -> csrf
            		.csrfTokenRepository(new HttpSessionCsrfTokenRepository()) // セッションでCSRFトークンを管理
            )
            // 認可ルールの設定
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/register",  // API登録
                    "/login",         // ログインページ
                    "user/register/**", // ユーザー登録関連のリクエストを許可
                    "/css/**", "/js/**"  // 静的リソース
                ).permitAll() // 認証不要
                .anyRequest().authenticated() // その他のリクエストは認証が必要
            )
            // フォームログインの設定
            .formLogin(form -> form
                .loginPage("/login") // ログインページ
                .defaultSuccessUrl("/home", true) // ログイン成功後のリダイレクト先
                .permitAll() // ログインフォーム自体は誰でもアクセス可能
            )
            // ログアウトの設定
            .logout(logout -> logout
                .logoutUrl("/logout") // ログアウトURL
                .logoutSuccessUrl("/login?logout") // ログアウト成功後のリダイレクト先
                .invalidateHttpSession(true) // セッションを無効化
                .deleteCookies("JSESSIONID", "XSRF-TOKEN") // クッキーを削除
                .permitAll() // ログアウトは誰でも可能
            );

        return http.build(); // SecurityFilterChainのビルド
    }

    // パスワードエンコーダーの設定（BCryptを使用）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 認証プロバイダーの設定
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // 認証マネージャーの設定
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        return http.getSharedObject(AuthenticationManager.class);
    }

    // セッションイベントの公開 (並行セッションの管理用)
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
