package com.example.demo.service;

import java.util.Collections;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.config.ApplicationContextProvider;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;

@Service // サービスとして認識
public class UserProfileDetailsService implements UserDetailsService {

	private final UserProfileRepository userProfileRepository;

    // コンストラクタで依存性注入
	 public UserProfileDetailsService(UserProfileRepository userProfileRepository) {
	        this.userProfileRepository = userProfileRepository;
	    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ログ: ユーザー名を出力
        System.out.println("[DEBUG] Attempting to load user: " + username);

        // ユーザー検索
        Optional<UserProfile> userOptional = userProfileRepository.findByUsername(username);

        // 結果が存在しない場合
        if (userOptional.isEmpty()) {
            System.out.println("[DEBUG] No user found for username: " + username);
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // ユーザーが存在する場合
        UserProfile user = userOptional.get();
        System.out.println("[DEBUG] Loaded user: " + user.getUsername());
        System.out.println("[DEBUG] User password (hashed): " + user.getPassword());

        // Spring コンテキストから PasswordEncoder を取得
        PasswordEncoder passwordEncoder = ApplicationContextProvider.getBean(PasswordEncoder.class);

        // テスト用パスワード (ログイン画面から入力されるはずの平文パスワード)
        String rawPassword = "password123"; // デバッグ用。通常は取得した平文パスワードに置き換える。
        boolean matches = passwordEncoder.matches(rawPassword, user.getPassword());
        System.out.println("[DEBUG] Does input password match hashed password?: " + matches);

        if (!matches) {
            System.out.println("[ERROR] Passwords do not match for username: " + username);
        }

        // UserDetailsを生成して返す
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
