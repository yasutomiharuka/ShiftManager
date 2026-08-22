package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * エラー画面への遷移を管理するController
 */
@Controller
public class ErrorPageController {

    /**
     * アクセス権限不足（403）画面を表示する
     *
     * @return 403エラー画面のテンプレート名
     */
    @GetMapping("/error/403")
    public String showAccessDeniedPage() {
        return "error/403";
    }
}