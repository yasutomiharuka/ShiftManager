package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
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

/**
 * ユーザー情報を操作するREST APIを提供するController。
 *
 * 【対応するAPI】
 * ・GET    /api/user/list
 *   ユーザー一覧を取得する。
 *
 * ・POST   /api/user/register
 *   新規ユーザーを登録する。
 *
 * ・GET    /api/user/{id}
 *   指定したユーザー情報を取得する。
 *
 * ・PUT    /api/user/{id}
 *   指定したユーザー情報を更新する。
 *
 * ・DELETE /api/user/{id}
 *   指定したユーザー情報を削除する。
 *
 * 【入力チェックの方針】
 * ・新規登録時は共通項目に加え、パスワードを必須とする。
 * ・更新時はパスワードを必須にしない。
 *
 * 【エラー応答の方針】
 * ・入力エラー             : 400 Bad Request
 * ・対象ユーザー不存在     : 404 Not Found
 * ・重複・DB制約違反       : 409 Conflict
 * ・想定外の保存・更新エラー : 500 Internal Server Error
 *
 * 【注意】
 * このクラスは@RestControllerのため、
 * エラー発生時にHTML画面へ直接戻す処理は行わない。
 *
 * APIの呼び出し元が、HTTPステータスとエラー内容を受け取り、
 * 必要に応じて元の画面にエラーメッセージを表示する。
 */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    // ユーザーAPIの処理状況やエラー内容を記録するLogger。
    private static final Logger logger =
            LoggerFactory.getLogger(
                    UserProfileController.class
            );

    // ユーザー情報の取得・登録・更新・削除を担当するService。
    private final UserProfileService userProfileService;

    /**
     * コンストラクタでUserProfileServiceを注入する。
     *
     * @param userProfileService ユーザー情報を扱うService
     */
    public UserProfileController(
            UserProfileService userProfileService
    ) {
        this.userProfileService = userProfileService;
    }

    /**
     * ユーザー一覧を取得する。
     *
     * 【API】
     * GET /api/user/list
     *
     * 【正常時】
     * ・HTTP 200 OK
     * ・登録済みユーザーのDTO一覧を返す。
     *
     * @return ユーザー情報のリスト
     */
    @GetMapping("/list")
    public ResponseEntity<List<UserProfileDto>> getAllUsers() {

        logger.debug(
                "Fetching all user profiles."
        );

        // Serviceを通じて登録済みユーザー一覧を取得する。
        List<UserProfileDto> users =
                userProfileService.getAllUserProfiles();

        logger.debug(
                "Fetched user profiles: count={}",
                users.size()
        );

        // HTTP 200 OKとユーザー一覧を返す。
        return ResponseEntity.ok(users);
    }

    /**
     * ユーザー登録処理。
     *
     * 【API】
     * POST /api/user/register
     *
     * 【入力チェック】
     * Default.class:
     * ・ユーザー名
     * ・ユーザーロール
     * ・姓
     * ・名
     * ・生年月日
     * ・性別
     * ・雇用形態
     * ・所属
     *
     * UserProfileDto.Create.class:
     * ・パスワードの必須チェック
     * ・パスワードの最小文字数チェック
     *
     * 【正常時】
     * ・HTTP 201 Created
     *
     * 【異常時】
     * ・入力エラー             : HTTP 400 Bad Request
     * ・ユーザー名の重複など   : HTTP 409 Conflict
     * ・その他の保存エラー     : HTTP 500 Internal Server Error
     *
     * @param userProfileDto 登録するユーザー情報のDTO
     * @param bindingResult バリデーションエラー情報
     * @return 登録したユーザー情報またはエラー内容
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(

            // JSON形式のリクエストをUserProfileDtoへ変換する。
            //
            // Default.class:
            // 登録・編集共通の入力チェックを実行する。
            //
            // UserProfileDto.Create.class:
            // 新規登録時だけ、パスワード必須・4文字以上を検証する。
            @RequestBody
            @Validated({
                    Default.class,
                    UserProfileDto.Create.class
            })
            UserProfileDto userProfileDto,

            // バリデーション結果を格納する。
            //
            // BindingResultは検証対象の引数の直後に置く。
            BindingResult bindingResult
    ) {

        // パスワードをログへ出さないため、
        // DTO全体ではなくユーザー名だけを記録する。
        logger.debug(
                "Received registration request: username={}",
                userProfileDto.getUsername()
        );

        // 必須項目不足、文字数超過、パスワード未入力などの
        // バリデーションエラーがある場合。
        if (bindingResult.hasErrors()) {

            // エラー内容を項目名とメッセージのMapへ変換する。
            //
            // 例:
            // {
            //     "username": "ユーザー名は必須です。",
            //     "password": "パスワードは必須です。"
            // }
            Map<String, String> validationErrors =
                    getValidationErrors(bindingResult);

            // BindingResult#getAllErrors()をそのまま出力すると、
            // 入力値がログに含まれる場合がある。
            //
            // パスワードなどの情報を残さないよう、
            // エラーが発生した項目名だけを記録する。
            logger.warn(
                    "Registration validation errors: fields={}",
                    validationErrors.keySet()
            );

            // HTTP 400 Bad Requestと項目別エラーを返す。
            return ResponseEntity
                    .badRequest()
                    .body(validationErrors);
        }

        try {

            // ユーザー情報を保存する。
            //
            // パスワードのハッシュ化は
            // UserProfileService#saveUserProfileで実行される。
            UserProfileDto savedUser =
                    userProfileService.saveUserProfile(
                            userProfileDto
                    );

            // 念のため、APIレスポンスにパスワードを含めない。
            //
            // 現在のService実装ではDTO変換時にパスワードを設定しないが、
            // 将来の実装変更にも備えて明示的に除外する。
            savedUser.setPassword(null);

            logger.info(
                    "User successfully registered: userId={}, username={}",
                    savedUser.getId(),
                    savedUser.getUsername()
            );

            // HTTP 201 Createdと登録したユーザー情報を返す。
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(savedUser);

        } catch (DataIntegrityViolationException e) {

            // ユーザー名の重複など、
            // DBの一意制約や整合性制約に違反した場合。
            logger.error(
                    "Database constraint violation during user registration: username={}",
                    userProfileDto.getUsername(),
                    e
            );

            // HTTP 409 Conflictと利用者向けのエラー内容を返す。
            //
            // SQL文やDB例外の詳細はレスポンスに含めない。
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                            createErrorResponse(
                                    "ERR_USER_REGISTER_SAVE_FAILED",
                                    "ユーザーを登録できませんでした。"
                                            + "ユーザー名の重複など、"
                                            + "入力内容をご確認ください。"
                            )
                    );

        } catch (Exception e) {

            // DB接続エラーなど、その他の想定外の登録エラー。
            logger.error(
                    "Unexpected error during user registration: username={}",
                    userProfileDto.getUsername(),
                    e
            );

            // HTTP 500 Internal Server Errorと
            // 利用者向けのエラーメッセージを返す。
            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            createErrorResponse(
                                    "ERR_USER_REGISTER_SAVE_FAILED",
                                    "ユーザーの登録に失敗しました。"
                                            + "時間をおいて再度お試しください。"
                            )
                    );
        }
    }

    /**
     * ユーザー情報を取得する。
     *
     * 【API】
     * GET /api/user/{id}
     *
     * 【正常時】
     * ・HTTP 200 OK
     * ・指定したユーザー情報を返す。
     *
     * 【対象が存在しない場合】
     * ・HTTP 404 Not Found
     *
     * @param id ユーザーID
     * @return ユーザー情報またはエラーメッセージ
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(
            @PathVariable Long id
    ) {

        logger.debug(
                "Fetching user profile for ID: {}",
                id
        );

        // 指定されたIDのユーザー情報を取得する。
        Optional<UserProfileDto> userProfile =
                userProfileService.getUserProfileById(id);

        // 対象ユーザーが存在する場合はHTTP 200を返す。
        //
        // 存在しない場合は、HTTP 404と
        // 既存仕様のエラーメッセージを返す。
        return userProfile
                .<ResponseEntity<?>>map(
                        ResponseEntity::ok
                )
                .orElseGet(
                        () -> ResponseEntity
                                .status(
                                        HttpStatus.NOT_FOUND
                                )
                                .body(
                                        "User profile not found for ID: "
                                                + id
                                )
                );
    }

    /**
     * ユーザー情報を更新する。
     *
     * 【API】
     * PUT /api/user/{id}
     *
     * 【入力チェック】
     * 更新時は@Validを使用し、
     * Defaultグループの共通項目だけを検証する。
     *
     * UserProfileDto.Createグループを指定しないため、
     * パスワードが未入力でも更新できる。
     *
     * 【正常時】
     * ・HTTP 200 OK
     *
     * 【異常時】
     * ・入力エラー             : HTTP 400 Bad Request
     * ・対象ユーザー不存在     : HTTP 404 Not Found
     * ・ユーザー名の重複など   : HTTP 409 Conflict
     * ・その他の更新エラー     : HTTP 500 Internal Server Error
     *
     * @param id ユーザーID
     * @param userProfileDto 更新するユーザー情報のDTO
     * @param bindingResult バリデーションエラー情報
     * @return 更新後のユーザー情報またはエラー内容
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(

            // URLに含まれる更新対象ユーザーのID。
            @PathVariable Long id,

            // 更新時はDefaultグループのみを検証する。
            //
            // Createグループは適用されないため、
            // パスワードを指定しなくても更新できる。
            @RequestBody
            @Valid
            UserProfileDto userProfileDto,

            // 入力チェックの結果を格納する。
            BindingResult bindingResult
    ) {

        // DTO全体をログへ出力せず、
        // ユーザーIDとユーザー名だけを記録する。
        logger.debug(
                "Updating user profile: userId={}, username={}",
                id,
                userProfileDto.getUsername()
        );

        // ユーザー名、氏名、所属などに
        // 入力エラーがある場合はHTTP 400を返す。
        if (bindingResult.hasErrors()) {

            Map<String, String> validationErrors =
                    getValidationErrors(bindingResult);

            logger.warn(
                    "User update validation errors: userId={}, fields={}",
                    id,
                    validationErrors.keySet()
            );

            return ResponseEntity
                    .badRequest()
                    .body(validationErrors);
        }

        try {

            // 指定されたユーザー情報を更新する。
            //
            // 現在のService実装では、
            // 既存パスワードを変更せずに他の項目を更新する。
            Optional<UserProfileDto> updatedUser =
                    userProfileService.updateUserProfile(
                            id,
                            userProfileDto
                    );

            // 対象ユーザーが存在しない場合はHTTP 404を返す。
            if (updatedUser.isEmpty()) {

                logger.warn(
                    "User profile not found for update: userId={}",
                    id
                );

                // 既存APIとの互換性を保つため、
                // 対象不存在時の文字列レスポンスは変更しない。
                return ResponseEntity
                        .status(
                                HttpStatus.NOT_FOUND
                        )
                        .body(
                                "User profile not found for ID: "
                                        + id
                        );
            }

            // 更新後のユーザー情報を取得する。
            UserProfileDto savedUser =
                    updatedUser.get();

            // 念のため、APIレスポンスに
            // パスワードを含めない。
            savedUser.setPassword(null);

            logger.info(
                    "User profile updated successfully: userId={}, username={}",
                    savedUser.getId(),
                    savedUser.getUsername()
            );

            // HTTP 200 OKと更新後のユーザー情報を返す。
            return ResponseEntity.ok(savedUser);

        } catch (DataIntegrityViolationException e) {

            // 更新後のユーザー名が他のユーザーと重複するなど、
            // DB制約に違反した場合。
            logger.error(
                    "Database constraint violation during user update: userId={}, username={}",
                    id,
                    userProfileDto.getUsername(),
                    e
            );

            return ResponseEntity
                    .status(
                            HttpStatus.CONFLICT
                    )
                    .body(
                            createErrorResponse(
                                    "ERR_USER_UPDATE_FAILED",
                                    "ユーザー情報を更新できませんでした。"
                                            + "ユーザー名の重複など、"
                                            + "入力内容をご確認ください。"
                            )
                    );

        } catch (Exception e) {

            // その他の想定外の更新エラー。
            logger.error(
                    "Unexpected error during user update: userId={}",
                    id,
                    e
            );

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            createErrorResponse(
                                    "ERR_USER_UPDATE_FAILED",
                                    "ユーザー情報の更新に失敗しました。"
                                            + "時間をおいて再度お試しください。"
                            )
                    );
        }
    }

    /**
     * ユーザー情報を削除する。
     *
     * 【API】
     * DELETE /api/user/{id}
     *
     * 【正常時】
     * ・HTTP 204 No Content
     *
     * 【異常時】
     * ・対象ユーザー不存在     : HTTP 404 Not Found
     * ・関連データによる制約違反 : HTTP 409 Conflict
     * ・その他の削除エラー     : HTTP 500 Internal Server Error
     *
     * @param id ユーザーID
     * @return 削除結果のレスポンス
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserProfile(
            @PathVariable Long id
    ) {

        logger.debug(
                "Deleting user profile for ID: {}",
                id
        );

        try {

            // Serviceを通じて対象ユーザーを削除する。
            boolean isDeleted =
                    userProfileService.deleteUserProfile(
                            id
                    );

            // 対象ユーザーが存在しなかった場合。
            if (!isDeleted) {

                logger.warn(
                        "User profile not found for deletion: userId={}",
                        id
                );

                // 既存APIとの互換性を保つため、
                // 対象不存在時の文字列レスポンスは変更しない。
                return ResponseEntity
                        .status(
                                HttpStatus.NOT_FOUND
                        )
                        .body(
                                "User profile not found for ID: "
                                        + id
                        );
            }

            logger.info(
                    "User profile deleted successfully: userId={}",
                    id
            );

            // 削除成功時は本文なしのHTTP 204を返す。
            return ResponseEntity
                    .noContent()
                    .build();

        } catch (DataIntegrityViolationException e) {

            // 対象ユーザーに関連シフトなどが存在し、
            // DBの参照整合性制約により削除できない場合。
            logger.error(
                    "Database constraint violation during user deletion: userId={}",
                    id,
                    e
            );

            return ResponseEntity
                    .status(
                            HttpStatus.CONFLICT
                    )
                    .body(
                            createErrorResponse(
                                    "ERR_USER_DELETE_FAILED",
                                    "関連するデータが存在するため、"
                                            + "ユーザーを削除できませんでした。"
                            )
                    );

        } catch (Exception e) {

            // その他の想定外の削除エラー。
            logger.error(
                    "Unexpected error during user deletion: userId={}",
                    id,
                    e
            );

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            createErrorResponse(
                                    "ERR_USER_DELETE_FAILED",
                                    "ユーザーの削除に失敗しました。"
                                            + "時間をおいて再度お試しください。"
                            )
                    );
        }
    }

    /**
     * バリデーションエラー情報を整形する。
     *
     * 【返却例】
     *
     * {
     *     "username": "ユーザー名は必須です。",
     *     "password": "パスワードは4文字以上で入力してください。"
     * }
     *
     * 同じ項目に複数の入力エラーがある場合は、
     * 最初に取得したエラーメッセージを優先する。
     *
     * @param bindingResult バリデーションエラー情報
     * @return エラー内容を格納したMap
     */
    private Map<String, String> getValidationErrors(
            BindingResult bindingResult
    ) {

        // 項目名とエラーメッセージを保持するMap。
        Map<String, String> errors =
                new HashMap<>();

        // フィールド単位の入力エラーを順番に取り出す。
        bindingResult
                .getFieldErrors()
                .forEach(
                        error -> errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return errors;
    }

    /**
     * APIの保存・更新・削除エラー応答を作成する。
     *
     * 【返却例】
     *
     * {
     *     "errorCode": "ERR_USER_UPDATE_FAILED",
     *     "message": "ユーザー情報の更新に失敗しました。"
     * }
     *
     * 【注意】
     * ・SQL文、例外メッセージ、スタックトレースは返さない。
     * ・技術的な詳細はサーバーログだけに記録する。
     *
     * @param errorCode エラーの識別コード
     * @param message 利用者向けのエラーメッセージ
     * @return エラーコードとメッセージを格納したMap
     */
    private Map<String, String> createErrorResponse(

            String errorCode,

            String message
    ) {

        Map<String, String> errorResponse =
                new HashMap<>();

        // システム側でエラー種別を判別するためのコード。
        errorResponse.put(
                "errorCode",
                errorCode
        );

        // 画面やAPI利用者へ表示するメッセージ。
        errorResponse.put(
                "message",
                message
        );

        return errorResponse;
    }
}