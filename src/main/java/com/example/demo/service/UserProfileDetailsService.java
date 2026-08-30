package com.example.demo.service;

import java.util.Collections;
import java.util.Locale;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;

/**
 * ログイン認証に使用するユーザー情報を取得するService。
 *
 * active=trueのユーザーだけを認証対象とする。
 * 無効化されたユーザーの情報や過去シフトは削除しない。
 *
 * パスワードの照合はSpring Securityに任せる。
 * このクラスでは、入力パスワードの照合やログ出力を行わない。
 */
@Service
public class UserProfileDetailsService implements UserDetailsService {

    private final UserProfileRepository userProfileRepository;

    /**
     * コンストラクタでRepositoryを注入する。
     *
     * @param userProfileRepository ユーザー情報を取得するRepository
     */
    public UserProfileDetailsService(
            UserProfileRepository userProfileRepository
    ) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * ユーザー名から、認証に必要な情報を取得する。
     *
     * 【取得対象】
     * active=trueの有効なユーザーのみ。
     *
     * 【認証失敗】
     * ・ユーザーが存在しない場合
     * ・ユーザーが無効化されている場合
     *
     * @param username ログイン画面に入力されたユーザー名
     * @return Spring Securityが認証に使用するユーザー情報
     * @throws UsernameNotFoundException 認証対象が存在しない場合
     */
    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        // 無効化されたユーザーをログイン対象から除外する。
        //
        // ユーザー不存在と無効化は同じエラーとして扱い、
        // アカウントの存在や状態を外部へ知らせない。
        UserProfile user = userProfileRepository
                .findByUsernameAndActiveTrue(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "ユーザー名またはパスワードが正しくありません。"
                        )
                );

        // 権限が未設定の場合は、認証用ユーザー情報を生成しない。
        String role = user.getRole();

        if (role == null || role.isBlank()) {
            throw new UsernameNotFoundException(
                    "ユーザー名またはパスワードが正しくありません。"
            );
        }

        // Spring Securityの権限名をROLE_ADMIN / ROLE_USER形式に統一する。
        //
        // admin、ADMIN、ROLE_ADMINのいずれでも、
        // ROLE_が重複しないように変換する。
        String authority = role.trim().toUpperCase(Locale.ROOT);

        if (!authority.startsWith("ROLE_")) {
            authority = "ROLE_" + authority;
        }

        // DBに保存されているハッシュ化済みパスワードを渡す。
        //
        // ログイン画面の入力パスワードとの照合は、
        // SecurityConfigで設定した認証プロバイダーと
        // PasswordEncoderが実行する。
        //
        // 平文パスワードもハッシュ値もログには出力しない。
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singleton(
                        new SimpleGrantedAuthority(authority)
                )
        );
    }
}