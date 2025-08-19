package com.example.demo.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/* 
データベースに直接ユーザーを追加する際、ハッシュ化されたパスワードをSQL文に入力する必要がある
ハッシュ化されたパスワードを生成するために必要。テスト不要なら削除してもよい。
 */
public class PasswordHasher {
    public static void main(String[] args) {
        // パスワードの平文（ハッシュ化する前の値）
        String rawPassword = "password123"; // 例: "password123"

        // BCryptPasswordEncoder インスタンスを作成
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // ハッシュ化されたパスワードを生成
        String hashedPassword = encoder.encode(rawPassword);

        // ハッシュ化結果を出力
        System.out.println("Hashed Password: " + hashedPassword);
    }
}
