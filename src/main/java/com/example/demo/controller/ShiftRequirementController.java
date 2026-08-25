// src/main/java/com/example/demo/controller/ShiftRequirementController.java
package com.example.demo.controller;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.form.ShiftRequirementForm;
import com.example.demo.service.ShiftRequirementService;

/**
 * 【必要人員の登録用コントローラ】
 *
 * ・シフト生成画面（generate.html）の
 *   「必要人員」入力フォームからのPOSTを受け取る。
 *
 * ・ShiftRequirementServiceを通じて、
 *   必要人員（ShiftRequirement）を登録・更新する。
 *
 * ＜役割の整理＞
 *
 * ・URL:
 *    - /shift/requirement/save
 *      従来の保存用エンドポイント。
 *
 *    - /api/shift/requirement/save
 *      シフト保存と同様に/api配下へ寄せた保存用エンドポイント。
 *
 * ・HTTPメソッド:
 *    POST。
 *
 * ・リクエストボディ:
 *    @ModelAttribute("requirementForm")
 *    ShiftRequirementForm。
 *
 *    部署・年月・日付ごとの時間帯別必要人数を
 *    1か月分まとめて保持する。
 *
 * ・処理内容:
 *    - フォーム内容をサービス層へ委譲する。
 *    - DB上のShiftRequirementをアップサートする。
 *    - actionに応じて保存モードを切り替える。
 *    - 正常終了時はnoticeを設定する。
 *    - 入力エラー・保存エラー時はerrorMessageを設定する。
 *    - 保存できなかった入力内容をrequirementFormとして保持する。
 *    - 同じ部署・年月のシフト生成画面へリダイレクトする。
 *
 * ※シフト本体の自動生成とは処理を分離する。
 *
 *   これにより、「必要人員だけを先に登録・調整したい」
 *   という運用にも対応する。
 */
@Controller
public class ShiftRequirementController {

    // 必要人員の保存・確定・確定解除に関する処理状況を記録する。
    private static final Logger logger =
            LoggerFactory.getLogger(
                    ShiftRequirementController.class
            );

    // 必要人員の保存・確定・確定解除を担当するService。
    private final ShiftRequirementService shiftRequirementService;

    /**
     * コンストラクタでShiftRequirementServiceを注入する。
     *
     * @param shiftRequirementService 必要人員を扱うService
     */
    public ShiftRequirementController(
            ShiftRequirementService shiftRequirementService
    ) {

        this.shiftRequirementService =
                shiftRequirementService;
    }

    /**
     * シフト生成画面で入力された「必要人員」を保存する。
     *
     * ・画面側ではth:object="${requirementForm}"として
     *   バインドされたフォームをPOSTする。
     *
     * ・保存後は同じ部署・年月のシフト生成画面
     *   /shift/generateへリダイレクトする。
     *
     * ▼対応する画面側の仕様（generate.html）
     *
     * ・ボタン押下でactionパラメータを送る。
     *
     *   - DRAFT:
     *     必要人員を下書きとして一時保存する。
     *
     *   - CONFIRMED:
     *     必要人員を確定し、シフト生成に反映する。
     *
     *   - UNCONFIRM:
     *     確定済みの必要人員を下書きへ戻す。
     *
     * ※/api/shift/requirement/saveを正規のエンドポイントとして運用する。
     *
     *   既存の/shift/requirement/saveも、
     *   互換性のため同じ処理へ通す。
     *
     * 【入力エラー時】
     * ・部署未指定。
     * ・対象年・対象月の不正。
     * ・必要人員への負数入力。
     * ・必要人員の数値変換エラー。
     * ・actionに想定外の値が指定された場合。
     *
     * 【保存エラー時】
     * ・保存前のrequirementFormをFlashAttributeへ保持する。
     * ・errorMessageをFlashAttributeへ設定する。
     * ・同じ部署・対象年月の生成画面へ戻す。
     *
     * @param form 必要人員の入力フォーム
     * @param bindingResult フォームの変換・入力エラー情報
     * @param action 保存操作の種類
     * @param redirectAttributes リダイレクト時に引き継ぐ情報
     * @return シフト生成画面へのリダイレクト
     */
    @PostMapping({
            "/api/shift/requirement/save",
            "/shift/requirement/save"
    })
    public String saveRequirements(

            // generate.htmlのth:object="${requirementForm}"に合わせ、
            // フォームの属性名をrequirementFormに統一する。
            @ModelAttribute("requirementForm")
            ShiftRequirementForm form,

            // 必要人数の数値変換エラーや、
            // 年月の形式エラーを受け取る。
            //
            // BindingResultは対象フォーム引数の直後へ配置する。
            BindingResult bindingResult,

            // 既存仕様を維持し、
            // actionが省略された場合はDRAFTとして扱う。
            @RequestParam(
                    name = "action",
                    defaultValue = "DRAFT"
            )
            String action,

            // 部署・対象年月・通知・入力内容を
            // リダイレクト先へ引き継ぐ。
            RedirectAttributes redirectAttributes
    ) {

        // 入力された必要人員の件数を取得する。
        //
        // 日別・時間帯別の入力内容をすべてログへ出すと
        // 情報量が多いため、件数だけ記録する。
        int submittedRequirementCount =
                form != null
                        && form.getRequiredCounts() != null
                                ? form.getRequiredCounts().size()
                                : 0;

        logger.debug(
                "Saving shift requirements: "
                        + "department={}, year={}, month={}, "
                        + "action={}, submittedRequirementCount={}",
                form != null
                        ? form.getDepartment()
                        : null,
                form != null
                        ? form.getYear()
                        : null,
                form != null
                        ? form.getMonth()
                        : null,
                action,
                submittedRequirementCount
        );

        // =========================================================
        // 1. フォームの存在確認
        // =========================================================

        // 通常、Spring MVCがフォームを生成するため、
        // formがnullになることはない。
        //
        // ただし、予期しない呼び出しに備え、
        // フォームを取得できない場合は保存を行わない。
        if (form == null) {

            logger.warn(
                    "Requirement save failed because the form is missing."
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "必要人員の入力内容を取得できませんでした。"
                            + "画面を再表示して、"
                            + "もう一度お試しください。"
            );

            return "redirect:/shift/generate";
        }

        // =========================================================
        // 2. 部署の確認
        // =========================================================

        // 部署が指定されていない場合は、
        // どの部署の必要人員を保存するか特定できない。
        if (!StringUtils.hasText(
                form.getDepartment()
        )) {

            logger.warn(
                    "Requirement save failed because department is missing: "
                            + "year={}, month={}",
                    form.getYear(),
                    form.getMonth()
            );

            // 保存されなかった必要人員フォームを保持する。
            preserveRequirementForm(

                    form,

                    bindingResult,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "部署が指定されていません。"
                            + "部署を選択してから"
                            + "必要人員を保存してください。"
            );

            return redirectToGenerate(

                    form,

                    redirectAttributes
            );
        }

        // 部署コードの前後に余分な空白がある場合は取り除く。
        //
        // Service側とリダイレクト先の両方で、
        // 同じ部署コードを使用する。
        form.setDepartment(

                form.getDepartment().trim()
        );

        // =========================================================
        // 3. フォームの変換・入力エラー確認
        // =========================================================

        // 例：
        // ・必要人員欄に整数へ変換できない値が入力された。
        // ・yearまたはmonthに数値以外の値が送信された。
        //
        // BindingResultを受け取らない場合、
        // フォーム変換エラーがそのまま例外になる可能性がある。
        if (bindingResult.hasErrors()) {

            logger.warn(
                    "Requirement form binding errors: "
                            + "department={}, year={}, month={}, fields={}",
                    form.getDepartment(),
                    form.getYear(),
                    form.getMonth(),
                    bindingResult
                            .getFieldErrors()
                            .stream()
                            .map(
                                    fieldError ->
                                            fieldError.getField()
                            )
                            .toList()
            );

            // 入力済みフォームと変換エラー情報を保持する。
            //
            // BindingResultも引き継ぐことで、
            // 整数に変換できなかった入力値のエラー情報を
            // 生成画面で参照できる。
            preserveRequirementForm(

                    form,

                    bindingResult,

                    redirectAttributes
            );

            // 年月の変換に失敗した場合と、
            // 必要人員の入力に失敗した場合でメッセージを分ける。
            if (bindingResult.hasFieldErrors(
                    "year"
            ) || bindingResult.hasFieldErrors(
                    "month"
            )) {

                redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "対象年月が正しくありません。"
                            + "対象年月を確認してください。"
                );

            } else {

                redirectAttributes.addFlashAttribute(

                        "errorMessage",

                        "必要人員は0以上の整数で"
                                + "入力してください。"
                );
            }

            return redirectToGenerate(

                    form,

                    redirectAttributes
            );
        }

        // =========================================================
        // 4. 対象年・対象月の確認
        // =========================================================

        // /shift/generateではmonth=yyyy-MMを使用する。
        //
        // そのため、年は1〜9999、
        // 月は1〜12の範囲内であることを確認する。
        //
        // 例：
        // ・2026年8月  → 正常。
        // ・2026年0月  → エラー。
        // ・2026年13月 → エラー。
        if (!isValidYearMonth(
                form.getYear(),
                form.getMonth()
        )) {

            logger.warn(
                    "Invalid requirement target year or month: "
                            + "department={}, year={}, month={}",
                    form.getDepartment(),
                    form.getYear(),
                    form.getMonth()
            );

            // 必要人員の入力内容を保持する。
            preserveRequirementForm(

                    form,

                    bindingResult,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "対象年月が正しくありません。"
                            + "対象年月を確認してください。"
            );

            // 不正な年月はmonthパラメータへ設定しない。
            return redirectToGenerate(

                    form,

                    redirectAttributes
            );
        }

        // =========================================================
        // 5. 必要人員の入力値を確認する
        // =========================================================

        // 必要人員は日付・時間帯をキーとするMapで保持する。
        //
        // 例：
        // {
        //     "2026-08-01_9:00-14:00": 3,
        //     "2026-08-01_14:00-16:00": 2
        // }
        Map<String, Integer> requiredCounts =
                form.getRequiredCounts();

        // 未入力のnullは既存Serviceの仕様に従って扱う。
        //
        // 数値が入力されている場合は、
        // 0以上であることを確認する。
        boolean hasNegativeCount =
                requiredCounts != null
                        && requiredCounts
                                .values()
                                .stream()
                                .anyMatch(

                                        count -> count != null
                                                && count < 0
                                );

        // 1件でも負数が存在する場合は保存しない。
        if (hasNegativeCount) {

            logger.warn(
                    "Negative requirement count detected: "
                            + "department={}, year={}, month={}",
                    form.getDepartment(),
                    form.getYear(),
                    form.getMonth()
            );

            // 負数を含め、利用者が入力した内容を保持する。
            preserveRequirementForm(

                    form,

                    bindingResult,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "必要人員は0以上の数値で"
                            + "入力してください。"
            );

            return redirectToGenerate(

                    form,

                    redirectAttributes
            );
        }

        // =========================================================
        // 6. 保存操作の正規化
        // =========================================================

        // actionの前後の空白と大小文字の違いを吸収する。
        //
        // 例：
        // ・"draft"       → "DRAFT"
        // ・" confirmed " → "CONFIRMED"
        // ・"unconfirm"   → "UNCONFIRM"
        //
        // Locale.ROOTを使用することで、
        // サーバーの言語設定による変換の差を避ける。
        String normalizedAction =
                action == null
                        ? "DRAFT"
                        : action
                                .trim()
                                .toUpperCase(
                                        Locale.ROOT
                                );

        // =========================================================
        // 7. actionに応じて保存モードを切り替える
        // =========================================================

        try {
            switch (normalizedAction) {

            case "CONFIRMED":

                // 確定。
                //
                // シフト生成で参照する正式な必要人数として保存する。
                //
                // 実際の保存処理はServiceへ委譲する。
                shiftRequirementService
                        .saveConfirmedFromForm(
                                form
                        );

                redirectAttributes.addFlashAttribute(

                        "notice",

                        "必要人員を確定しました。"
                );

                logger.info(
                        "Shift requirements confirmed: "
                                + "department={}, year={}, month={}, "
                                + "submittedRequirementCount={}",
                        form.getDepartment(),
                        form.getYear(),
                        form.getMonth(),
                        submittedRequirementCount
                );

                break;

            case "UNCONFIRM":

                // 確定解除。
                //
                // 当月・部署の確定済み必要人員を、
                // 下書き状態へ戻す。
                shiftRequirementService.unconfirm(

                        form.getDepartment(),

                        form.getYear(),

                        form.getMonth()
                );

                redirectAttributes.addFlashAttribute(

                        "notice",

                        "必要人員の確定解除を行いました。"
                );

                logger.info(
                        "Shift requirement confirmation removed: "
                                + "department={}, year={}, month={}",
                        form.getDepartment(),
                        form.getYear(),
                        form.getMonth()
                );

                break;

            case "DRAFT":

                // 一時保存。
                //
                // 入力された必要人員を、
                // 下書き状態として保存する。
                shiftRequirementService
                        .saveDraftFromForm(
                                form
                        );

                redirectAttributes.addFlashAttribute(

                        "notice",

                        "必要人員を一時保存しました。"
                );

                logger.info(
                        "Shift requirements saved as draft: "
                                + "department={}, year={}, month={}, "
                                + "submittedRequirementCount={}",
                        form.getDepartment(),
                        form.getYear(),
                        form.getMonth(),
                        submittedRequirementCount
                );

                break;

            default:

                // 想定外のaction。
                //
                // 既存コードでは、
                // CONFIRMED・UNCONFIRM以外を一律でDRAFTとしていた。
                //
                // ただし、不正値や画面側の設定ミスを見逃さないため、
                // 未対応のactionでは保存を実行しない。
                logger.warn(
                        "Unsupported requirement save action: "
                                + "department={}, year={}, month={}, action={}",
                        form.getDepartment(),
                        form.getYear(),
                        form.getMonth(),
                        normalizedAction
                );

                // 保存されなかった必要人員の入力内容を保持する。
                preserveRequirementForm(

                        form,

                        bindingResult,

                        redirectAttributes
                );

                redirectAttributes.addFlashAttribute(

                        "errorMessage",

                        "必要人員の保存操作を"
                                + "判別できませんでした。"
                                + "もう一度お試しください。"
                );

                return redirectToGenerate(

                        form,

                        redirectAttributes
                );
            }

        } catch (DataIntegrityViolationException e) {

            // 必要人員の保存時に、
            // DBの一意制約や整合性制約へ違反した場合。
            //
            // SQL文や例外の詳細は画面に表示せず、
            // サーバーログへ記録する。
            logger.error(
                    "Database constraint violation during requirement save: "
                            + "department={}, year={}, month={}, action={}",
                    form.getDepartment(),
                    form.getYear(),
                    form.getMonth(),
                    normalizedAction,
                    e
            );

            // 保存できなかった必要人員の入力内容を保持する。
            preserveRequirementForm(

                    form,

                    bindingResult,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "必要人員を保存できませんでした。"
                            + "入力内容を確認してから"
                            + "再度お試しください。"
            );

        } catch (IllegalArgumentException e) {

            // Service側で部署・年月・時間帯などの値が
            // 不正と判断された場合。
            logger.warn(
                    "Invalid requirement save parameters: "
                            + "department={}, year={}, month={}, action={}",
                    form.getDepartment(),
                    form.getYear(),
                    form.getMonth(),
                    normalizedAction,
                    e
            );

            // 入力済みフォームを保持する。
            preserveRequirementForm(

                    form,

                    bindingResult,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "必要人員の入力内容に誤りがあります。"
                            + "部署、対象年月、人数を"
                            + "ご確認ください。"
            );

        } catch (Exception e) {

            // その他の予期しない保存・確定・解除エラー。
            //
            // 例：
            // ・DB接続エラー。
            // ・保存中の予期しない例外。
            // ・確定解除処理の例外。
            logger.error(
                    "Unexpected error during requirement save: "
                            + "department={}, year={}, month={}, action={}",
                    form.getDepartment(),
                    form.getYear(),
                    form.getMonth(),
                    normalizedAction,
                    e
            );

            // 保存できなかった入力値を、
            // 次の画面表示までFlashAttributeで保持する。
            preserveRequirementForm(

                    form,

                    bindingResult,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "必要人員の保存に失敗しました。"
                            + "入力内容を保持していますので、"
                            + "確認してから再度お試しください。"
            );
        }

        // =========================================================
        // 8. 同じ部署・対象年月の生成画面へ戻す
        // =========================================================

        // /shift/generate側は、
        // month=yyyy-MM形式で対象年月を受け取る。
        //
        // 部署と年月は文字列連結ではなく、
        // RedirectAttributes#addAttributeで安全に付与する。
        return redirectToGenerate(

                form,

                redirectAttributes
        );
    }

    /**
     * 保存失敗時の必要人員フォームを保持する。
     *
     * 【保持対象】
     * ・対象部署。
     * ・対象年。
     * ・対象月。
     * ・日付・時間帯ごとの必要人数。
     * ・必要に応じてフォームの入力エラー情報。
     *
     * 【復元の流れ】
     * 1. 保存・確定・確定解除でエラーが発生する。
     *
     * 2. requirementFormをFlashAttributeとして保存する。
     *
     * 3. /shift/generateへリダイレクトする。
     *
     * 4. ShiftGenerationController#showGeneratePageで
     *    FlashAttributeのrequirementFormを取得する。
     *
     * 5. DB上の必要人員より、
     *    保存失敗直前の入力内容を優先して画面へ表示する。
     *
     * 【BindingResultについて】
     * 数値変換に失敗した場合などは、
     * 入力エラー情報も一緒に引き継ぐ。
     *
     * Spring MVCでは以下の名前でBindingResultを保持する。
     *
     * org.springframework.validation.BindingResult.requirementForm
     *
     * @param form 保存失敗時の必要人員フォーム
     * @param bindingResult 入力・変換エラー情報
     * @param redirectAttributes リダイレクト時に引き継ぐ属性
     */
    private void preserveRequirementForm(

            ShiftRequirementForm form,

            BindingResult bindingResult,

            RedirectAttributes redirectAttributes
    ) {

        if (form == null) {
            return;
        }

        // 入力済みの必要人員フォームを一時的に保持する。
        redirectAttributes.addFlashAttribute(

                "requirementForm",

                form
        );

        // フォームに入力エラーがある場合は、
        // エラー情報も同じ属性名に関連付けて保持する。
        //
        // これにより、再表示時にThymeleafの
        // th:errorsなどから入力エラーを参照できる。
        if (bindingResult != null
                && bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute(

                    BindingResult.MODEL_KEY_PREFIX
                            + "requirementForm",

                    bindingResult
            );
        }

        logger.debug(
                "Preserved requirement form after an error: "
                        + "department={}, year={}, month={}, count={}",
                form.getDepartment(),
                form.getYear(),
                form.getMonth(),
                form.getRequiredCounts() != null
                        ? form.getRequiredCounts().size()
                        : 0
        );
    }

    /**
     * 対象年・対象月が有効かを確認する。
     *
     * 【許可する範囲】
     * ・年：1〜9999。
     * ・月：1〜12。
     *
     * 年はシフト生成画面で使用するyyyy-MM形式に合わせ、
     * 4桁以内に制限する。
     *
     * @param year 対象年
     * @param month 対象月
     * @return 対象年月が有効な場合はtrue
     */
    private boolean isValidYearMonth(

            int year,

            int month
    ) {

        return year >= 1
                && year <= 9999
                && month >= 1
                && month <= 12;
    }

    /**
     * シフト生成画面へリダイレクトする。
     *
     * 【URLパラメータ】
     * ・department：対象部署。
     * ・month：yyyy-MM形式の対象年月。
     *
     * 【遷移例】
     * /shift/generate?department=amami&month=2026-08
     *
     * 【入力エラー時】
     * ・部署が未設定の場合はdepartmentを付与しない。
     * ・対象年月が不正な場合はmonthを付与しない。
     *
     * requirementForm、notice、errorMessageは
     * FlashAttributeとして別途引き継ぐ。
     *
     * @param form 必要人員入力フォーム
     * @param redirectAttributes リダイレクト先へ渡す属性
     * @return シフト生成画面へのリダイレクト
     */
    private String redirectToGenerate(

            ShiftRequirementForm form,

            RedirectAttributes redirectAttributes
    ) {

        if (form == null) {
            return "redirect:/shift/generate";
        }

        // 部署が設定されている場合だけ、
        // URLパラメータへ引き継ぐ。
        if (StringUtils.hasText(
                form.getDepartment()
        )) {

            redirectAttributes.addAttribute(

                    "department",

                    form.getDepartment().trim()
            );
        }

        // 有効な対象年月だけをyyyy-MM形式でURLへ設定する。
        //
        // 不正な月をそのまま渡すと、
        // ShiftGenerationController側で再度変換エラーになるため、
        // その場合はmonthを設定しない。
        if (isValidYearMonth(

                form.getYear(),

                form.getMonth()
        )) {

            // 例：
            // year=2026、month=8 → "2026-08"。
            String monthParam =
                    String.format(

                            "%04d-%02d",

                            form.getYear(),

                            form.getMonth()
                    );

            redirectAttributes.addAttribute(

                    "month",

                    monthParam
            );
        }

        return "redirect:/shift/generate";
    }
}