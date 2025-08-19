package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.service.UserProfileService;

@Controller
@RequestMapping("/user")
public class UserProfileViewController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileViewController.class);
    private final UserProfileService userProfileService;

    public UserProfileViewController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * ユーザー一覧画面の表示
     * 
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return ユーザー一覧画面のテンプレート名
     */
    @GetMapping("/list")
    public String showUserList(Model model) {
        List<UserProfileDto> users = userProfileService.getAllUserProfiles();
        model.addAttribute("users", users);
        return "user/list";
    }

    /**
     * ユーザー詳細画面の表示
     * 
     * @param id ユーザーID
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return ユーザー詳細画面のテンプレート名
     */
    @GetMapping("/detail/{id}")
    public String showUserDetail(@PathVariable Long id, Model model) {
        UserProfileDto user = userProfileService.getUserProfileById(id)
                .map(this::convertToDto)
                .orElse(null);

        if (user == null) {
            logger.warn("User not found: ID {}", id);
            return "redirect:/user/list";	
        }

        model.addAttribute("user", user);
        return "user/detail";
    }

    /**
     * ユーザー編集画面の表示
     */
    @GetMapping("/edit/{id}")
    public String showEditUser(@PathVariable Long id, Model model) {
        UserProfileDto user = userProfileService.getUserProfileById(id)
                .map(this::convertToDto)
                .orElse(null);

        if (user == null) {
            logger.warn("User not found: ID {}", id);
            return "redirect:/user/list";
        }

        model.addAttribute("user", user);
        return "user/edit";
    }

    /**
     * 編集確認画面の表示
     */
    @PostMapping("/edit/confirm")
    public String confirmEditUser(
            @ModelAttribute("user") UserProfileDto user,
            BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            return "user/edit";
        }

        model.addAttribute("user", user);
        return "user/edit/confirm";
    }

    /**
     * 編集完了処理
     */
    @PostMapping("/edit/complete")
    public String completeEditUser(
            @ModelAttribute("user") UserProfileDto user,  // `userProfileDto` を `user` に統一
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "user/edit";
        }

        userProfileService.updateUserProfile(user.getId(), user); // 変数名も `user` に変更
        return "user/edit/complete";
    }

    
    /**
     * 削除確認画面の表示
     * 
     * @param id ユーザーID
     * @param model ThymeleafのModelオブジェクト
     * @return 削除確認画面のテンプレート名
     */
    @GetMapping("/delete/confirm/{id}")
    public String showDeleteConfirmPage(@PathVariable Long id, Model model) {
        UserProfileDto user = userProfileService.getUserProfileById(id)
                .map(this::convertToDto)
                .orElse(null);

        if (user == null) {
            return "redirect:/user/list";
        }

        model.addAttribute("user", user);
        return "user/delete/confirm";
    }

    /**
     * 削除処理
     * 
     * @param id ユーザーID
     * @return 削除完了画面またはリスト画面へのリダイレクト
     */
    @PostMapping("/delete/{id}") // フォーム送信の場合は @PostMapping
    public String deleteUser(@PathVariable Long id) {
        boolean isDeleted = userProfileService.deleteUserProfile(id);
        return isDeleted ? "redirect:/user/delete/complete" : "redirect:/user/list";
    }

    /**
     * 削除完了画面の表示
     * 
     * @return 削除完了画面のテンプレート名
     */
    @GetMapping("/delete/complete")
    public String showDeleteCompletePage() {
        return "user/delete/complete";
    }


    /**
     * エンティティをDTOに変換するヘルパーメソッド
     */
    private UserProfileDto convertToDto(UserProfileDto userProfile) {
        return userProfile;
    }
}
