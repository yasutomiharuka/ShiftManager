package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.service.UserProfileService;

/**
 * ユーザー登録画面の表示、入力内容の確認、登録処理を担当するController。
 *
 * 【画面遷移】
 * 1. GET  /user/register
 *    ユーザー登録画面を表示する。
 *
 * 2. POST /user/register/confirm
 *    入力内容を検証し、問題がなければ登録確認画面を表示する。
 *
 * 3. POST /user/register/complete
 *    入力内容を再度検証し、ユーザー情報を保存する。
 *
 * 【エラー発生時の方針】
 * ・入力エラー時は登録画面へ戻す。
 * ・DB保存エラー時も登録画面へ戻す。
 * ・登録画面を再表示する際は、パート勤務時間の選択肢を再設定する。
 * ・保存失敗時はパスワードを再表示せず、再入力を求める。
 *
 * 【パスワードの検証】
 * UserProfileDto.Createグループを指定することで、
 * 新規登録時のみパスワードの必須・最小文字数チェックを実行する。
 */
@Controller
@RequestMapping("/user/register")
public class RegistrationController {

    // Loggerを使用してデバッグ情報やエラー情報を出力する。
    private static final Logger logger =
            LoggerFactory.getLogger(RegistrationController.class);

    // サービスクラスを利用してビジネスロジックを実行する。
    private final UserProfileService userProfileService;

    /**
     * コンストラクタでUserProfileServiceを注入する。
     *
     * @param userProfileService ユーザー登録・更新・取得を担当するService
     */
    public RegistrationController(
            UserProfileService userProfileService
    ) {
        this.userProfileService = userProfileService;
    }

    /**
     * ユーザー登録画面を表示する処理。
     *
     * 【処理内容】
     * ・新規登録用のUserProfileDtoをModelへ設定する。
     * ・パート勤務者の固定勤務時間で使用する選択肢を設定する。
     *
     * 【補足】
     * すでにuserProfileDtoがModelに存在する場合は上書きしない。
     * これにより、将来的にFlashAttributeなどで入力値を
     * 引き継ぐ場合にも対応できる。
     *
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return ユーザー登録画面のテンプレート名
     */
    @GetMapping
    public String showRegisterPage(Model model) {

        logger.debug("Displaying registration page.");

        // 空のUserProfileDtoオブジェクトをモデルに
        // "userProfileDto"という名前で追加する。
        //
        // エラー後などで既にDTOが設定されている場合は、
        // 入力内容を消さないように上書きしない。
        if (!model.containsAttribute("userProfileDto")) {

            model.addAttribute(
                    "userProfileDto",
                    new UserProfileDto()
            );
        }

        // パート勤務者の勤務時間選択肢を設定する。
        //
        // 登録画面の初期表示時だけでなく、
        // エラー後の再表示時も同じ選択肢を使用する。
        addTimeOptions(model);

        // 登録画面のテンプレート名を返却する。
        return "user/register";
    }

    /**
     * 確認画面を表示する処理。
     *
     * 【バリデーション】
     * ・Default.class
     *   ユーザー名、氏名、生年月日、性別、所属など、
     *   登録・編集で共通して必要な項目を検証する。
     *
     * ・UserProfileDto.Create.class
     *   新規登録時だけ必要なパスワードの必須・最小文字数を検証する。
     *
     * 【入力エラー時】
     * ・ユーザー登録画面を再表示する。
     * ・入力済みの内容を保持する。
     * ・パート勤務時間の選択肢を再設定する。
     * ・画面上部に共通エラーメッセージを表示する。
     *
     * @param userProfileDto ユーザーが入力したデータを格納するDTO
     * @param bindingResult バリデーション結果
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return 確認画面、または入力エラー時は登録画面
     */
    @PostMapping("/confirm")
    public String showConfirmPage(

            // フォームデータをバインドする。
            // 属性名はregister.htmlのth:objectに合わせて
            // "userProfileDto"に統一する。
            @ModelAttribute("userProfileDto")
            @Validated({
                    Default.class,
                    UserProfileDto.Create.class
            })
            UserProfileDto userProfileDto,

            // バリデーション結果を格納する。
            //
            // BindingResultは検証対象の引数の直後に配置する。
            BindingResult bindingResult,

            Model model
    ) {

        // パスワードなどの入力値をログに残さないよう、
        // DTO全体ではなくユーザー名だけを出力する。
        logger.debug(
                "Received data for confirmation: username={}",
                userProfileDto.getUsername()
        );

        // バリデーションエラーがある場合は登録画面に戻る。
        if (bindingResult.hasErrors()) {

            logger.warn(
                    "Validation errors during registration confirmation: {}",
                    bindingResult.getAllErrors()
            );

            // 登録画面では曜日別の勤務時間選択肢を使用するため、
            // エラーによる画面再表示時も改めて設定する。
            //
            // これを省略すると、パート勤務時間のセレクトボックスに
            // 選択肢が表示されなくなる。
            addTimeOptions(model);

            // register.html側で表示する共通エラーメッセージ。
            //
            // 項目別のエラーは、既存のth:errorsで表示する。
            model.addAttribute(
                    "errorMessage",
                    "入力内容に誤りがあります。内容をご確認ください。"
            );

            // 入力済みのuserProfileDtoとBindingResultを保持したまま、
            // 登録画面のテンプレート名を返却する。
            return "user/register";
        }

        // モデルにユーザーデータを追加して確認画面に渡す。
        //
        // @ModelAttributeによってすでにModelへ設定されるが、
        // 既存コードの構成を維持するため明示的に設定する。
        model.addAttribute(
                "userProfileDto",
                userProfileDto
        );

        // 確認画面のテンプレート名を返却する。
        return "user/register/confirm";
    }

    /**
     * ユーザー登録処理を実行し、完了画面を表示する処理。
     *
     * 【処理内容】
     * 1. 確認画面から送信された入力内容を再検証する。
     * 2. 問題がなければUserProfileServiceで保存する。
     * 3. 保存が成功した場合は登録完了画面を表示する。
     *
     * 【エラー時】
     * ・バリデーションエラーの場合は登録画面に戻す。
     * ・ユーザー名の重複などのDB制約違反も登録画面に戻す。
     * ・その他の保存エラーも登録画面に戻す。
     * ・保存失敗後のパスワードは再表示せず、再入力を求める。
     *
     * @param userProfileDto ユーザーが入力したデータを格納するDTO
     * @param bindingResult バリデーション結果
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     * @return 登録完了画面、またはエラー時は登録画面
     */
    @PostMapping("/complete")
    public String registerUser(

            // フォームデータをバインドする。
            // 確認画面のhidden項目から受け取る属性名も
            // "userProfileDto"に統一する。
            @ModelAttribute("userProfileDto")
            @Validated({
                    Default.class,
                    UserProfileDto.Create.class
            })
            UserProfileDto userProfileDto,

            // バリデーション結果を格納する。
            BindingResult bindingResult,

            Model model
    ) {

        // DTO全体は出力せず、ユーザー名だけをログに残す。
        logger.debug(
                "Processing registration: username={}",
                userProfileDto.getUsername()
        );

        // バリデーションエラーがある場合は登録画面に戻る。
        if (bindingResult.hasErrors()) {

            logger.warn(
                    "Validation errors during registration: {}",
                    bindingResult.getAllErrors()
            );

            // パート勤務時間の選択肢を登録画面に再設定する。
            addTimeOptions(model);

            // 確認画面から戻る場合はパスワードを画面に再表示せず、
            // 登録画面で改めて入力してもらう。
            userProfileDto.setPassword(null);

            model.addAttribute(
                    "errorMessage",
                    "入力内容に誤りがあります。"
                            + "内容を確認し、パスワードを再入力してください。"
            );

            // 入力内容を保持したまま登録画面を返す。
            return "user/register";
        }

        try {

            // サービスクラスを通してUserProfileDtoを
            // UserProfileエンティティに変換し、DBへ保存する。
            //
            // パスワードのハッシュ化はUserProfileService側で実行する。
            userProfileService.saveUserProfile(
                    userProfileDto
            );

            // 登録成功の情報ログ。
            // パスワードを含むDTO全体は出力しない。
            logger.info(
                    "User registered successfully: username={}",
                    userProfileDto.getUsername()
            );

            // 完了画面のテンプレート名を返却する。
            return "user/register/complete";

        } catch (DataIntegrityViolationException e) {

            // ユーザー名の重複など、
            // DBの一意制約・NOT NULL制約に違反した場合。
            //
            // 技術的な詳細は画面に出さず、サーバーログに記録する。
            logger.error(
                    "Database constraint violation during registration: username={}",
                    userProfileDto.getUsername(),
                    e
            );

            // 登録画面のパート勤務時間選択肢を復元する。
            addTimeOptions(model);

            // 登録失敗時はパスワードを再表示しない。
            userProfileDto.setPassword(null);

            model.addAttribute(
                    "errorMessage",
                    "ユーザーを登録できませんでした。"
                            + "ユーザー名の重複など、入力内容をご確認ください。"
                            + "パスワードは再入力してください。"
            );

            // 登録画面へ戻す。
            return "user/register";

        } catch (Exception e) {

            // その他の予期しない保存エラー。
            //
            // 画面上には例外の詳細を表示せず、
            // 再試行可能な利用者向けメッセージを表示する。
            logger.error(
                    "Unexpected error during registration: username={}",
                    userProfileDto.getUsername(),
                    e
            );

            // 登録画面のパート勤務時間選択肢を復元する。
            addTimeOptions(model);

            // パスワードを消去し、再入力を求める。
            userProfileDto.setPassword(null);

            model.addAttribute(
                    "errorMessage",
                    "ユーザーの登録に失敗しました。"
                            + "入力内容を確認し、パスワードを再入力してから"
                            + "もう一度お試しください。"
            );

            // エラー画面ではなく、元のユーザー登録画面へ戻す。
            return "user/register";
        }
    }

    /**
     * パート勤務者の固定勤務時間で使用する選択肢をModelへ設定する。
     *
     * 【設定する値】
     * ・09:00
     * ・09:30
     * ・10:00
     * ・10:30
     * ・……
     * ・18:00
     * ・18:30
     *
     * 【使用タイミング】
     * ・登録画面の初期表示。
     * ・確認画面への遷移前に入力エラーが発生した場合。
     * ・登録完了処理で入力エラーが発生した場合。
     * ・ユーザー登録の保存処理が失敗した場合。
     *
     * @param model Thymeleafに渡すデータを保持するModelオブジェクト
     */
    private void addTimeOptions(Model model) {

        // 時間の選択肢を格納するリスト。
        List<String> timeOptions = new ArrayList<>();

        // 09:00〜18:30を30分刻みで作成する。
        //
        // 既存コードの選択肢を変更しないよう、
        // 18:30もそのまま含める。
        for (int hour = 9; hour <= 18; hour++) {

            // 毎時00分。
            timeOptions.add(
                    String.format("%02d:00", hour)
            );

            // 毎時30分。
            timeOptions.add(
                    String.format("%02d:30", hour)
            );
        }

        // register.htmlの勤務時間セレクトボックスで使用する。
        model.addAttribute(
                "timeOptions",
                timeOptions
        );
    }
}