package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration // 設定クラスとしてSpringコンテナに認識させる
public class UserConfig {

    @Bean // ユーザー情報を管理するInMemoryUserDetailsManagerを定義
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        // ユーザー情報を作成
        UserDetails user = User.withUsername("user") // ユーザー名を設定
                .password(passwordEncoder.encode("password")) // パスワードをエンコードして設定
                .roles("USER") // ユーザーロールを設定
                .build();

        UserDetails admin = User.withUsername("admin") // 管理者ユーザーの情報
                .password(passwordEncoder.encode("admin")) // パスワードをエンコードして設定
                .roles("ADMIN") // 管理者ロールを設定
                .build();

        // 作成したユーザー情報をInMemoryUserDetailsManagerに登録
        return new InMemoryUserDetailsManager(user, admin);
    }
}
