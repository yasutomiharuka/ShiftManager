package com.example.demo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.example.demo.service.UserProfileDetailsService;

@Configuration
public class SecurityConfig {

    private final UserProfileDetailsService userDetailsService;

    public SecurityConfig(
            UserProfileDetailsService userDetailsService
    ) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("guestUserDetailsService")
            InMemoryUserDetailsManager guestUserDetailsService
    ) throws Exception {

        http
            /*
             * DBに登録されている通常ユーザーと、
             * メモリ上のゲストユーザーの両方を認証対象にする。
             */
            .authenticationProvider(
                    databaseAuthenticationProvider()
            )
            .authenticationProvider(
                    guestAuthenticationProvider(
                            guestUserDetailsService
                    )
            )

            /*
             * 同じゲストアカウントを複数人が利用できるよう、
             * 同時セッション数を10件まで許可する。
             */
            .sessionManagement(session -> session
                .maximumSessions(10)
                .maxSessionsPreventsLogin(false)
                .and()
                .sessionFixation().migrateSession()
            )

            .csrf(csrf -> csrf
                .csrfTokenRepository(
                        new HttpSessionCsrfTokenRepository()
                )
            )

            .authorizeHttpRequests(auth -> auth
                /*
                 * ログイン画面、エラー画面、CSSなどは
                 * ログインしていない状態でも利用可能。
                 */
                .requestMatchers(
                        "/login",
                        "/error/**",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                ).permitAll()

                /*
                 * シフト管理とユーザー管理を含む、
                 * それ以外の機能はログイン後に利用可能。
                 *
                 * GUESTも認証済みユーザーとして扱われるため、
                 * 登録・編集・無効化を含む全機能を利用できる。
                 */
                .anyRequest().authenticated()
            )

            .exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
            )

            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies(
                        "JSESSIONID",
                        "XSRF-TOKEN"
                )
                .permitAll()
            );

        return http.build();
    }

    /**
     * パスワードをBCrypt形式でハッシュ化する。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * user_profilesテーブルに登録された
     * 通常ユーザーを認証する。
     */
    @Bean
    public DaoAuthenticationProvider
            databaseAuthenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * UserConfigで設定したゲストを認証する。
     */
    @Bean
    public DaoAuthenticationProvider guestAuthenticationProvider(
            @Qualifier("guestUserDetailsService")
            InMemoryUserDetailsManager guestUserDetailsService
    ) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(
                guestUserDetailsService
        );
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * 同時ログインセッションを管理する。
     */
    @Bean
    public HttpSessionEventPublisher
            httpSessionEventPublisher() {

        return new HttpSessionEventPublisher();
    }
}