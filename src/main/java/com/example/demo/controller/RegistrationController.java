package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.service.UserProfileService;

@Controller
@RequestMapping("/user/register")
public class RegistrationController {

    // Loggerを使用してデバッグ情報を出力
    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    // サービスクラスを利用してビジネスロジックを実行
    private final UserProfileService userProfileService;

    // コンストラクタでUserProfileServiceを注入
    public RegistrationController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * ユーザー登録画面を表示する処理
     * 
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return ユーザー登録画面のテンプレート名
     */
    @GetMapping
    public String showRegisterPage(Model model) {
        logger.debug("Displaying registration page."); // デバッグログ
        // 空のUserProfileDtoオブジェクトをモデルに "userProfileDto" という名前で追加
        model.addAttribute("userProfileDto", new UserProfileDto());

        // 時間の選択肢（9:00〜:00、30分刻み）
        List<String> timeOptions = new ArrayList<>();
        for (int h = 9; h <= 18; h++) {
            timeOptions.add(String.format("%02d:00", h));
            timeOptions.add(String.format("%02d:30", h));
        }
        model.addAttribute("timeOptions", timeOptions);        
        return "user/register"; // 登録画面のテンプレート名を返却
    }

    /**
     * 確認画面を表示する処理
     * 
     * @param userProfileDto ユーザーが入力したデータを格納するUserProfileDtoオブジェクト
     * @param bindingResult バリデーション結果を保持
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return 確認画面のテンプレート名または登録画面（エラー時）
     */
    @PostMapping("/confirm")
    public String showConfirmPage(
            @ModelAttribute("userProfileDto") @Valid UserProfileDto userProfileDto, // フォームデータをバインド（属性名をuserProfileDtoに統一）
            BindingResult bindingResult, // バリデーション結果を格納
            Model model) {
        logger.debug("Received data for confirmation: {}", userProfileDto); // デバッグログ

        // バリデーションエラーがある場合は登録画面に戻る
        if (bindingResult.hasErrors()) {
            logger.error("Validation errors: {}", bindingResult.getAllErrors()); // エラーログ
            return "user/register"; // 登録画面のテンプレート名を返却
        }

        // モデルにユーザーデータを追加（属性名をuserProfileDtoに統一）して確認画面に渡す
        model.addAttribute("userProfileDto", userProfileDto);
        return "user/register/confirm"; // 確認画面のテンプレート名を返却
    }

    /**
     * ユーザー登録処理を実行し、完了画面を表示する処理
     * 
     * @param userProfileDto ユーザーが入力したデータを格納するUserProfileDtoオブジェクト
     * @param bindingResult バリデーション結果を保持
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return 完了画面のテンプレート名または登録画面（エラー時）
     */
    @PostMapping("/complete")
    public String registerUser(
            @ModelAttribute("userProfileDto") @Valid UserProfileDto userProfileDto, // フォームデータをバインド（属性名をuserProfileDtoに統一）
            BindingResult bindingResult, // バリデーション結果を格納
            Model model) {
        logger.debug("Processing registration: {}", userProfileDto); // デバッグログ

        // バリデーションエラーがある場合は登録画面に戻る
        if (bindingResult.hasErrors()) {
            logger.error("Validation errors during registration: {}", bindingResult.getAllErrors()); // エラーログ
            return "user/register"; // 登録画面のテンプレート名を返却
        }

        // サービスクラスを通してUserProfileDtoをUserエンティティに変換・保存する
        userProfileService.saveUserProfile(userProfileDto);
        logger.info("User registered successfully: {}", userProfileDto); // 登録成功の情報ログ

        return "user/register/complete"; // 完了画面のテンプレート名を返却
    }
}
