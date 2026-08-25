package com.example.demo.controller;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

/**
 * ユーザー情報に関する各画面の表示と操作を担当するController。
 *
 * 【対象画面】
 * ・ユーザー一覧画面
 * ・ユーザー詳細画面
 * ・ユーザー編集画面
 * ・ユーザー編集確認画面
 * ・ユーザー編集完了画面
 * ・ユーザー削除確認画面
 * ・ユーザー削除完了画面
 *
 * 【編集処理の画面遷移】
 * 1. GET  /user/edit/{id}
 *    ユーザー編集画面を表示する。
 *
 * 2. POST /user/edit/confirm
 *    入力内容を検証し、問題がなければ編集確認画面を表示する。
 *
 * 3. POST /user/edit/complete
 *    入力内容を再検証し、ユーザー情報を更新する。
 *
 * 【編集エラー時の方針】
 * ・入力エラー時は編集画面へ戻す。
 * ・更新対象が存在しない場合も編集画面へ戻す。
 * ・ユーザー名の重複などのDB制約違反時も編集画面へ戻す。
 * ・その他の保存エラーも編集画面へ戻す。
 * ・編集済みの入力内容とエラーメッセージを保持する。
 *
 * 【パスワードについて】
 * 編集時は@Validを使用し、Defaultグループだけを検証する。
 *
 * UserProfileDto.Createグループは指定しないため、
 * パスワード未入力でもユーザー情報を編集できる。
 */
@Controller
@RequestMapping("/user")
public class UserProfileViewController {

    // ユーザー画面の処理状況やエラー内容を記録するLogger。
    private static final Logger logger =
            LoggerFactory.getLogger(
                    UserProfileViewController.class
            );

    // ユーザー情報の取得・更新・削除を担当するService。
    private final UserProfileService userProfileService;

    /**
     * コンストラクタでUserProfileServiceを注入する。
     *
     * @param userProfileService ユーザー情報を扱うService
     */
    public UserProfileViewController(
            UserProfileService userProfileService
    ) {
        this.userProfileService = userProfileService;
    }

    /**
     * ユーザー一覧画面の表示。
     *
     * 【URL】
     * GET /user/list
     *
     * 【処理内容】
     * ・登録済みユーザー一覧を取得する。
     * ・Thymeleafテンプレートへusersとして渡す。
     *
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return ユーザー一覧画面のテンプレート名
     */
    @GetMapping("/list")
    public String showUserList(Model model) {

        logger.debug(
                "Displaying user list page."
        );

        // Serviceを通じて登録済みユーザー一覧を取得する。
        List<UserProfileDto> users =
                userProfileService.getAllUserProfiles();

        // Thymeleafの一覧表示で使用するデータをModelへ設定する。
        model.addAttribute(
                "users",
                users
        );

        logger.debug(
                "User list loaded: count={}",
                users.size()
        );

        // ユーザー一覧画面を表示する。
        return "user/list";
    }

    /**
     * ユーザー詳細画面の表示。
     *
     * 【URL】
     * GET /user/detail/{id}
     *
     * 【処理内容】
     * ・指定IDのユーザー情報を取得する。
     * ・対象ユーザーが存在する場合は詳細画面を表示する。
     * ・対象ユーザーが存在しない場合は一覧画面へ戻す。
     *
     * @param id ユーザーID
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return ユーザー詳細画面、またはユーザー一覧画面へのリダイレクト
     */
    @GetMapping("/detail/{id}")
    public String showUserDetail(

            @PathVariable Long id,

            Model model
    ) {

        logger.debug(
                "Displaying user detail page: userId={}",
                id
        );

        // 指定したIDに該当するユーザー情報を取得する。
        //
        // convertToDtoは、既存実装との整合性を維持するため、
        // これまでどおり経由する。
        UserProfileDto user =
                userProfileService
                        .getUserProfileById(id)
                        .map(this::convertToDto)
                        .orElse(null);

        // 対象ユーザーが存在しない場合は一覧画面へ戻す。
        if (user == null) {

            logger.warn(
                    "User not found for detail page: userId={}",
                    id
            );

            return "redirect:/user/list";
        }

        // 詳細画面で使用するユーザー情報をModelへ設定する。
        model.addAttribute(
                "user",
                user
        );

        return "user/detail";
    }

    /**
     * ユーザー編集画面の表示。
     *
     * 【URL】
     * GET /user/edit/{id}
     *
     * 【処理内容】
     * ・指定IDのユーザー情報を取得する。
     * ・編集フォームへ初期値として設定する。
     * ・対象ユーザーが存在しない場合は一覧画面へ戻す。
     *
     * @param id ユーザーID
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return ユーザー編集画面、またはユーザー一覧画面へのリダイレクト
     */
    @GetMapping("/edit/{id}")
    public String showEditUser(

            @PathVariable Long id,

            Model model
    ) {

        logger.debug(
                "Displaying user edit page: userId={}",
                id
        );

        // 指定したユーザーIDに対応する編集対象データを取得する。
        UserProfileDto user =
                userProfileService
                        .getUserProfileById(id)
                        .map(this::convertToDto)
                        .orElse(null);

        // 編集対象のユーザーが存在しない場合は一覧画面へ戻す。
        if (user == null) {

            logger.warn(
                    "User not found for edit page: userId={}",
                    id
            );

            return "redirect:/user/list";
        }

        // edit.htmlのth:object="${user}"で使用するため、
        // 属性名を"user"に統一する。
        model.addAttribute(
                "user",
                user
        );

        return "user/edit";
    }

    /**
     * 編集確認画面の表示。
     *
     * 【URL】
     * POST /user/edit/confirm
     *
     * 【入力チェック】
     * @Validにより、UserProfileDtoのDefaultグループを検証する。
     *
     * 検証対象の例：
     * ・ユーザー名
     * ・ユーザーロール
     * ・姓
     * ・名
     * ・生年月日
     * ・性別
     * ・雇用形態
     * ・所属
     *
     * パスワードはUserProfileDto.Createグループに属するため、
     * 編集時の@Validでは必須チェックを実行しない。
     *
     * 【入力エラー時】
     * ・編集画面へ戻す。
     * ・入力済みの内容を保持する。
     * ・項目別エラーを表示する。
     * ・画面上部へ共通エラーメッセージを表示する。
     *
     * @param user 編集フォームから送信されたユーザー情報
     * @param bindingResult バリデーション結果
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return 編集確認画面、または入力エラー時は編集画面
     */
    @PostMapping("/edit/confirm")
    public String confirmEditUser(

            // edit.htmlのth:object="${user}"に合わせ、
            // Modelの属性名を"user"に統一する。
            //
            // @Validを付けることで、
            // DTOに定義したDefaultグループの入力チェックが実行される。
            @ModelAttribute("user")
            @Valid
            UserProfileDto user,

            // バリデーション結果を格納する。
            //
            // BindingResultは、検証対象のuserの直後に置く。
            BindingResult bindingResult,

            Model model
    ) {

        logger.debug(
                "Received user edit confirmation request: userId={}, username={}",
                user.getId(),
                user.getUsername()
        );

        // ユーザーIDは編集対象を特定するために必要。
        //
        // edit.htmlのhidden項目が正常に送信されなかった場合も、
        // 確認画面へ進ませず編集画面へ戻す。
        if (user.getId() == null) {

            logger.warn(
                    "User edit confirmation failed because user ID is missing."
            );

            model.addAttribute(
                    "errorMessage",
                    "編集対象のユーザーを特定できませんでした。"
                            + "ユーザー一覧から再度お試しください。"
            );

            return "user/edit";
        }

        // バリデーションエラーがある場合は編集画面に戻る。
        if (bindingResult.hasErrors()) {

            // 入力値をログへ出しすぎないよう、
            // エラーが発生した項目名だけを記録する。
            logger.warn(
                    "User edit validation errors: userId={}, fields={}",
                    user.getId(),
                    bindingResult
                            .getFieldErrors()
                            .stream()
                            .map(
                                    fieldError ->
                                            fieldError.getField()
                            )
                            .toList()
            );

            // edit.htmlの画面上部に表示する共通エラーメッセージ。
            //
            // 項目別のエラーは既存のth:errorsで表示する。
            model.addAttribute(
                    "errorMessage",
                    "入力内容に誤りがあります。"
                            + "内容をご確認ください。"
            );

            // @ModelAttribute("user")によって入力済みの値が
            // Modelに保持されるため、そのまま編集画面を再表示する。
            return "user/edit";
        }

        // モデルにユーザーデータを追加して確認画面に渡す。
        //
        // @ModelAttributeによってすでに設定されているが、
        // 既存コードの構成を維持するため明示的に追加する。
        model.addAttribute(
                "user",
                user
        );

        // 入力内容に問題がなければ編集確認画面を表示する。
        return "user/edit/confirm";
    }

    /**
     * 編集完了処理。
     *
     * 【URL】
     * POST /user/edit/complete
     *
     * 【処理内容】
     * 1. 確認画面から送信された入力内容を再検証する。
     * 2. 編集対象ユーザーが存在することを確認する。
     * 3. UserProfileServiceを通してユーザー情報を更新する。
     * 4. 更新成功時は編集完了画面を表示する。
     *
     * 【エラー時】
     * ・入力エラー                 : 編集画面へ戻す。
     * ・ユーザーID未設定           : 編集画面へ戻す。
     * ・更新対象ユーザー不存在     : 編集画面へ戻す。
     * ・ユーザー名の重複など       : 編集画面へ戻す。
     * ・その他の保存・更新エラー   : 編集画面へ戻す。
     *
     * @param user 編集フォームから送信されたユーザー情報
     * @param bindingResult バリデーション結果
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return 編集完了画面、またはエラー時は編集画面
     */
    @PostMapping("/edit/complete")
    public String completeEditUser(

            // 既存コードと同じく、
            // "userProfileDto"ではなく"user"に属性名を統一する。
            @ModelAttribute("user")
            @Valid
            UserProfileDto user,

            // 編集確認画面から送信された内容の
            // バリデーション結果を格納する。
            BindingResult bindingResult,

            // 保存エラー時にedit.htmlへエラーメッセージを渡すため、
            // 既存メソッドへModel引数を追加する。
            Model model
    ) {

        logger.debug(
                "Processing user update: userId={}, username={}",
                user.getId(),
                user.getUsername()
        );

        // 更新対象ユーザーを特定できない場合は、
        // 更新処理を実行せず編集画面へ戻す。
        if (user.getId() == null) {

            logger.warn(
                    "User update failed because user ID is missing."
            );

            model.addAttribute(
                    "errorMessage",
                    "編集対象のユーザーを特定できませんでした。"
                            + "ユーザー一覧から再度お試しください。"
            );

            return "user/edit";
        }

        // 確認画面経由であっても、
        // hidden項目の不足や値の改変に備えて再度検証する。
        if (bindingResult.hasErrors()) {

            logger.warn(
                    "Validation errors during user update: userId={}, fields={}",
                    user.getId(),
                    bindingResult
                            .getFieldErrors()
                            .stream()
                            .map(
                                    fieldError ->
                                            fieldError.getField()
                            )
                            .toList()
            );

            model.addAttribute(
                    "errorMessage",
                    "入力内容に誤りがあります。"
                            + "内容をご確認ください。"
            );

            // 入力内容を保持したまま編集画面を再表示する。
            return "user/edit";
        }

        try {
            // Serviceを通してユーザー情報を更新する。
            //
            // 既存のUserProfileServiceでは、
            // パスワードを変更せず、その他の項目を更新する。
            Optional<UserProfileDto> updatedUser =
                    userProfileService.updateUserProfile(

                            // 更新対象のユーザーID。
                            user.getId(),

                            // 編集画面から送信されたユーザー情報。
                            user
                    );

            // ServiceがOptional.empty()を返した場合、
            // 更新対象ユーザーが存在しない。
            //
            // 既存実装では戻り値を確認せず完了画面を表示していたため、
            // 存在しないユーザーでも更新成功扱いになる可能性があった。
            if (updatedUser.isEmpty()) {

                logger.warn(
                        "User not found during update: userId={}",
                        user.getId()
                );

                model.addAttribute(
                        "errorMessage",
                        "更新対象のユーザーが見つかりませんでした。"
                                + "ユーザー一覧をご確認ください。"
                );

                // 完了画面には遷移せず、編集画面に戻る。
                return "user/edit";
            }

            logger.info(
                    "User updated successfully: userId={}, username={}",
                    user.getId(),
                    user.getUsername()
            );

            // 更新が成功した場合のみ編集完了画面を表示する。
            return "user/edit/complete";

        } catch (DataIntegrityViolationException e) {

            // ユーザー名の重複など、
            // DBの一意制約・整合性制約に違反した場合。
            //
            // SQL文や例外の技術的な内容は画面に表示せず、
            // サーバーログだけに記録する。
            logger.error(
                    "Database constraint violation during user update: "
                            + "userId={}, username={}",
                    user.getId(),
                    user.getUsername(),
                    e
            );

            // 編集画面の上部に利用者向けエラーを表示する。
            model.addAttribute(
                    "errorMessage",
                    "ユーザー情報を更新できませんでした。"
                            + "ユーザー名の重複など、"
                            + "入力内容をご確認ください。"
            );

            // 更新前に入力した内容を保持したまま、
            // 元の編集画面へ戻す。
            return "user/edit";

        } catch (Exception e) {

            // DB接続エラーなど、
            // その他の予期しない更新エラー。
            logger.error(
                    "Unexpected error during user update: userId={}",
                    user.getId(),
                    e
            );

            model.addAttribute(
                    "errorMessage",
                    "ユーザー情報の更新に失敗しました。"
                            + "内容を確認し、"
                            + "もう一度お試しください。"
            );

            // 共通エラー画面ではなく、
            // 元のユーザー編集画面へ戻す。
            return "user/edit";
        }
    }

    /**
     * 削除確認画面の表示。
     *
     * 【URL】
     * GET /user/delete/confirm/{id}
     *
     * 【処理内容】
     * ・指定IDのユーザー情報を取得する。
     * ・対象ユーザーが存在する場合は削除確認画面を表示する。
     * ・対象ユーザーが存在しない場合は一覧画面へ戻す。
     *
     * @param id ユーザーID
     * @param model ThymeleafのModelオブジェクト
     * @return 削除確認画面、またはユーザー一覧画面へのリダイレクト
     */
    @GetMapping("/delete/confirm/{id}")
    public String showDeleteConfirmPage(

            @PathVariable Long id,

            Model model
    ) {

        logger.debug(
                "Displaying user delete confirmation page: userId={}",
                id
        );

        // 削除対象のユーザー情報を取得する。
        UserProfileDto user =
                userProfileService
                        .getUserProfileById(id)
                        .map(this::convertToDto)
                        .orElse(null);

        // 削除対象が存在しない場合は一覧画面へ戻す。
        if (user == null) {

            logger.warn(
                    "User not found for delete confirmation: userId={}",
                    id
            );

            return "redirect:/user/list";
        }

        // 削除確認画面で使用するユーザー情報をModelへ設定する。
        model.addAttribute(
                "user",
                user
        );

        return "user/delete/confirm";
    }

    /**
     * 削除処理。
     *
     * 【URL】
     * POST /user/delete/{id}
     *
     * 【正常時】
     * ・削除完了画面へリダイレクトする。
     *
     * 【対象が存在しない場合】
     * ・ユーザー一覧画面へリダイレクトする。
     *
     * ※既存の削除処理と画面遷移は変更しない。
     *
     * @param id ユーザーID
     * @return 削除完了画面またはユーザー一覧画面へのリダイレクト
     */
    @PostMapping("/delete/{id}") // フォーム送信の場合は@PostMapping
    public String deleteUser(
            @PathVariable Long id
    ) {

        logger.debug(
                "Processing user deletion: userId={}",
                id
        );

        // Serviceを通じて指定ユーザーを削除する。
        boolean isDeleted =
                userProfileService.deleteUserProfile(
                        id
                );

        // 削除成功時は完了画面へ移動する。
        if (isDeleted) {

            logger.info(
                    "User deleted successfully: userId={}",
                    id
            );

            return "redirect:/user/delete/complete";
        }

        // 対象ユーザーが存在しなかった場合は一覧画面へ戻す。
        logger.warn(
                "User not found during deletion: userId={}",
                id
        );

        return "redirect:/user/list";
    }

    /**
     * 削除完了画面の表示。
     *
     * 【URL】
     * GET /user/delete/complete
     *
     * @return 削除完了画面のテンプレート名
     */
    @GetMapping("/delete/complete")
    public String showDeleteCompletePage() {

        logger.debug(
                "Displaying user delete completion page."
        );

        return "user/delete/complete";
    }

    /**
     * エンティティをDTOに変換するヘルパーメソッド。
     *
     * 現在のUserProfileService#getUserProfileByIdは
     * UserProfileDtoを返すため、そのまま返却する。
     *
     * 将来的にServiceの戻り値がエンティティへ変更された場合は、
     * この箇所に変換処理を追加できる。
     *
     * @param userProfile ユーザー情報のDTO
     * @return 画面表示用のユーザー情報DTO
     */
    private UserProfileDto convertToDto(
            UserProfileDto userProfile
    ) {
        return userProfile;
    }
}