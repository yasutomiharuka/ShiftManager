package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.service.UserProfileService;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * ユーザー一覧の取得
     * 
     * @return ユーザー情報のリスト
     */
    @GetMapping("/list")
    public ResponseEntity<List<UserProfileDto>> getAllUsers() {
        List<UserProfileDto> users = userProfileService.getAllUserProfiles();
        return ResponseEntity.ok(users);
    }

    /**
     * ユーザー登録処理
     * 
     * @param userProfileDto - 登録するユーザー情報のDTO
     * @param bindingResult - バリデーションエラー情報
     * @return 登録したユーザー情報またはエラー内容
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestBody @Valid UserProfileDto userProfileDto,
            BindingResult bindingResult) {

        logger.debug("Received registration request: {}", userProfileDto);

        if (bindingResult.hasErrors()) {
            logger.error("Validation errors: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(getValidationErrors(bindingResult));
        }

        UserProfileDto savedUser = userProfileService.saveUserProfile(userProfileDto);
        logger.info("User successfully registered: {}", savedUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    /**
     * ユーザー情報の取得
     * 
     * @param id - ユーザーID
     * @return ユーザー情報またはエラーメッセージ
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        logger.debug("Fetching user profile for ID: {}", id);
        Optional<UserProfileDto> userProfile = userProfileService.getUserProfileById(id);
        
        return userProfile.<ResponseEntity<?>>map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                         .body("User profile not found for ID: " + id));
    }

    /**
     * ユーザー情報の更新
     * 
     * @param id - ユーザーID
     * @param userProfileDto - 更新するユーザー情報のDTO
     * @param bindingResult - バリデーションエラー情報
     * @return 更新後のユーザー情報またはエラーメッセージ
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long id,
            @RequestBody @Valid UserProfileDto userProfileDto,
            BindingResult bindingResult) {

        logger.debug("Updating user profile for ID: {} with data: {}", id, userProfileDto);

        if (bindingResult.hasErrors()) {
            logger.error("Validation errors: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(getValidationErrors(bindingResult));
        }

        Optional<UserProfileDto> updatedUser = userProfileService.updateUserProfile(id, userProfileDto);

        return updatedUser.<ResponseEntity<?>>map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                         .body("User profile not found for ID: " + id));
    }

    /**
     * ユーザー情報の削除
     * 
     * @param id - ユーザーID
     * @return 削除結果のレスポンス
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserProfile(@PathVariable Long id) {
        logger.debug("Deleting user profile for ID: {}", id);

        boolean isDeleted = userProfileService.deleteUserProfile(id);
        return isDeleted ? ResponseEntity.noContent().build()
                         : ResponseEntity.status(HttpStatus.NOT_FOUND).body("User profile not found for ID: " + id);
    }

    /**
     * バリデーションエラー情報を整形
     * 
     * @param bindingResult - バリデーションエラー情報
     * @return エラー内容を格納したMap
     */
    private Map<String, String> getValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return errors;
    }
}
