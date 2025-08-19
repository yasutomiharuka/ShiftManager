package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "error", required = false) String error,
                        Model model,
                        HttpServletRequest request) {
        // ログアウト後のメッセージを表示
        if (logout != null) {
            model.addAttribute("message", "ログアウトしました");
        }
        // ログインエラー時のメッセージを表示
        if (error != null) {
            model.addAttribute("error", "ログインに失敗しました。もう一度試してください。");
        }
        // CSRFトークンを取得してビューに渡す
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
        return "login";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
