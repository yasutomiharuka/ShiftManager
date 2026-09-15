package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * DBに登録しないゲストアカウントを設定する。
 *
 * ゲストはuser_profilesテーブルに存在しないため、
 * 職員一覧およびシフト生成対象には含まれない。
 */
@Configuration
public class UserConfig {

    @Bean
    public InMemoryUserDetailsManager guestUserDetailsService(
            PasswordEncoder passwordEncoder
    ) {

        UserDetails guest = User.withUsername("guest")
                .password(passwordEncoder.encode("1234"))
                .roles("GUEST")
                .build();

        return new InMemoryUserDetailsManager(guest);
    }
}