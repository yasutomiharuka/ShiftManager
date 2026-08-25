package com.example.demo.controller;

import java.util.LinkedHashMap;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.model.Shift;
import com.example.demo.service.ShiftService;

/**
 * ShiftEditController
 *
 * シフトの保存・編集処理を担当するコントローラ。
 *
 * - 一時保存（DRAFT）
 * - 確定保存（CONFIRMED）
 * - 確定解除（CONFIRMED → DRAFT）
 *
 * 画面はgenerate.html（シフト生成画面）から送られてくる
 * フォームを処理する。
 *
 * 保存処理が完了したらPRGパターンで、
 * 再度 /shift/generate にリダイレクトする。
 *
 * ※生成画面の表示はShiftGenerationControllerが担当する。
 *
 * ▼変更点
 * ・フロント側のボタンはすべて
 *   /api/shift/request/save にPOSTする。
 *
 * ・name="action" の値で保存処理を分岐する。
 *
 * ・本クラスのルートは /api/shift/request とする。
 *
 * ・入力エラーや保存エラーが発生した場合は、
 *   入力済みシフトを保持して元の生成画面へ戻す。
 *
 * ▼ステータスの考え方
 * ・DRAFT
 *   下書き。編集途中のシフトを一時保存する。
 *
 * ・CONFIRMED
 *   確定。その時点での正式なシフトとして保存する。
 *
 *   画面表示時もCONFIRMEDを優先して採用する。
 *   優先順位はShiftService側のstatusRankで制御する。
 *
 * ・UNCONFIRM
 *   確定されたシフトをDRAFTへ戻す操作。
 *
 * ▼エラー発生時の保持内容
 * ・部署：department
 * ・対象年月：targetMonth
 * ・変更済みシフト：submittedShifts
 * ・利用者向けエラー：errorMessage
 *
 * submittedShiftsはFlashAttributeとして一時的に保持し、
 * ShiftGenerationControllerでDB上のシフト情報へ重ねて表示する。
 */
@Controller
@RequestMapping("/api/shift/request") // /shift → /api/shift/request
public class ShiftEditController {

    // 保存・確定・確定解除の処理状況とエラーを記録するLogger。
    private static final Logger logger =
            LoggerFactory.getLogger(
                    ShiftEditController.class
            );

    // シフトの保存・確定解除を担当するService。
    private final ShiftService shiftService;

    /**
     * コンストラクタでShiftServiceを注入する。
     *
     * @param shiftService シフト保存・確定解除を担当するService
     */
    public ShiftEditController(
            ShiftService shiftService
    ) {
        this.shiftService = shiftService;
    }

    /**
     * 共通保存エンドポイント。
     *
     * - action=DRAFT
     *   一時保存（下書き）。
     *
     * - action=CONFIRMED
     *   確定保存（正式シフトとして保存）。
     *
     * - action=UNCONFIRM
     *   確定解除（確定済みデータを下書きへ戻す）。
     *
     * 例）generate.htmlのボタン：
     *
     * <button
     *     type="submit"
     *     formaction="/api/shift/request/save"
     *     name="action"
     *     value="DRAFT">
     *     一時保存
     * </button>
     *
     * <button
     *     type="submit"
     *     formaction="/api/shift/request/save"
     *     name="action"
     *     value="CONFIRMED">
     *     確定
     * </button>
     *
     * <button
     *     type="submit"
     *     formaction="/api/shift/request/save"
     *     name="action"
     *     value="UNCONFIRM">
     *     確定解除
     * </button>
     *
     * 【正常時】
     * ・保存結果をnoticeとして画面へ渡す。
     * ・同じ部署・対象年月のシフト生成画面へ戻る。
     *
     * 【異常時】
     * ・変更済みシフトをsubmittedShiftsとして保持する。
     * ・エラーメッセージをerrorMessageとして画面へ渡す。
     * ・同じ部署・対象年月のシフト生成画面へ戻る。
     *
     * @param form シフト生成画面から送信された入力フォーム
     * @param bindingResult フォームの変換・バインドエラー情報
     * @param action 保存操作の種類
     * @param departmentParam 保険として受け取る部署パラメータ
     * @param ra リダイレクト先へ引き継ぐ値とメッセージ
     * @return シフト生成画面へのリダイレクト
     */
    @PostMapping("/save")
    public String save(

            // generate.htmlから送信された部署・対象年月・
            // シフト入力内容をShiftGenerationFormへ格納する。
            @ModelAttribute
            ShiftGenerationForm form,

            // targetMonthなどの形式変換に失敗した場合、
            // 共通エラー画面へ進ませず生成画面へ戻すために追加する。
            //
            // BindingResultは対象フォーム引数の直後に配置する。
            BindingResult bindingResult,

            // 既存仕様に合わせ、actionが送信されなかった場合は
            // CONFIRMEDを既定値として扱う。
            @RequestParam(
                    name = "action",
                    defaultValue = "CONFIRMED"
            )
            String action,

            // フォーム内のdepartmentが取得できない場合に備え、
            // 同名のリクエストパラメータも受け取る。
            @RequestParam(
                    name = "department",
                    required = false
            )
            String departmentParam,

            // リダイレクト先へ部署・対象年月・通知・入力内容を渡す。
            RedirectAttributes ra
    ) {

        // 受け取ったシフト件数を確認する。
        //
        // シフト内容全体をログへ出力すると、
        // 職員ごとの勤務情報が大量に記録されるため、
        // 実際の入力値ではなく件数だけを出力する。
        int submittedShiftCount =
                form != null && form.getShifts() != null
                        ? form.getShifts().size()
                        : 0;

        logger.debug(
                "ShiftEditController.save: "
                        + "action={}, deptParam={}, formDept={}, "
                        + "targetMonth={}, submittedShiftCount={}",
                action,
                departmentParam,
                form != null
                        ? form.getDepartment()
                        : null,
                form != null
                        ? form.getTargetMonth()
                        : null,
                submittedShiftCount
        );

        // =========================================================
        // 1. 部署の取得
        // =========================================================

        // 部署の取得はフォーム内のdepartmentを優先し、
        // 取得できない場合はリクエストパラメータを保険として使う。
        String resolvedDepartment = null;

        if (form != null
                && StringUtils.hasText(
                        form.getDepartment()
                )) {

            // フォームに設定された部署を優先する。
            resolvedDepartment =
                    form.getDepartment().trim();

        } else if (StringUtils.hasText(
                departmentParam
        )) {

            // フォーム側から取得できなかった場合は、
            // リクエストパラメータの部署を使用する。
            resolvedDepartment =
                    departmentParam.trim();
        }

        // =========================================================
        // 2. フォーム・部署の入力確認
        // =========================================================

        // 通常、Spring MVCがフォームオブジェクトを生成するため
        // formがnullになることはない。
        //
        // ただし、将来の実装変更や予期しない呼び出しにも備え、
        // nullの場合は保存処理を行わず生成画面へ戻す。
        if (form == null) {

            logger.warn(
                    "Shift save failed because the form is missing."
            );

            // 有効な部署パラメータが取得できる場合は、
            // 生成画面の再表示に引き継ぐ。
            if (StringUtils.hasText(
                    resolvedDepartment
            )) {

                ra.addAttribute(
                        "department",
                        resolvedDepartment
                );
            }

            ra.addFlashAttribute(
                    "errorMessage",
                    "シフトの入力内容を取得できませんでした。"
                            + "画面を再表示して、もう一度お試しください。"
            );

            return "redirect:/shift/generate";
        }

        // 部署が取れなければ保存不可。
        if (!StringUtils.hasText(
                resolvedDepartment
        )) {

            logger.warn(
                    "Shift save failed because department is missing."
            );

            // 入力済みシフトがある場合は保持する。
            preserveSubmittedShifts(
                    form,
                    ra
            );

            // 正常通知のnoticeではなく、
            // エラー表示専用のerrorMessageへ設定する。
            ra.addFlashAttribute(
                    "errorMessage",
                    "部署が選択されていません。"
                            + "部署を選択してから保存してください。"
            );

            return redirectToGenerate(
                    form,
                    ra
            );
        }

        // サービス層ではform.departmentを必ず参照するため、
        // フォーム側にも確定した部署を設定する。
        form.setDepartment(
                resolvedDepartment
        );

        // =========================================================
        // 3. フォーム変換・対象年月の確認
        // =========================================================

        // targetMonthの変換失敗など、
        // フォームのバインドエラーを確認する。
        //
        // 例：
        // ・targetMonthがyyyy-MM形式ではない。
        // ・存在しない年月が送信された。
        if (bindingResult.hasErrors()) {

            logger.warn(
                    "Shift form binding errors: department={}, fields={}",
                    resolvedDepartment,
                    bindingResult
                            .getFieldErrors()
                            .stream()
                            .map(
                                    fieldError ->
                                            fieldError.getField()
                            )
                            .toList()
            );

            // 保存できなかったシフト入力内容を保持する。
            preserveSubmittedShifts(
                    form,
                    ra
            );

            // 対象年月の変換エラーがある場合は、
            // 利用者に原因がわかるメッセージを表示する。
            if (bindingResult.hasFieldErrors(
                    "targetMonth"
            )) {

                ra.addFlashAttribute(
                        "errorMessage",
                        "対象年月を正しく指定してください。"
                );

            } else {

                ra.addFlashAttribute(
                        "errorMessage",
                        "シフトの入力内容に誤りがあります。"
                                + "内容を確認してから"
                                + "再度お試しください。"
                );
            }

            // 変換できた部署・対象年月だけをURLへ引き継ぐ。
            //
            // 不正な年月はform.getTargetMonth()がnullになるため、
            // redirectToGenerateでmonthパラメータへ設定されない。
            return redirectToGenerate(
                    form,
                    ra
            );
        }

        // 対象年月が取れなければ保存不可。
        //
        // 対象月がないまま保存すると、
        // Service側で対象期間を特定できない。
        if (form.getTargetMonth() == null) {

            logger.warn(
                    "Shift save failed because target month is missing: "
                            + "department={}",
                    resolvedDepartment
            );

            // 保存前の入力済みシフトを保持する。
            preserveSubmittedShifts(
                    form,
                    ra
            );

            ra.addFlashAttribute(
                    "errorMessage",
                    "対象年月が指定されていません。"
                            + "対象年月を選択してから保存してください。"
            );

            return redirectToGenerate(
                    form,
                    ra
            );
        }

        // =========================================================
        // 4. 保存操作の正規化
        // =========================================================

        // actionの大小文字や前後の空白を吸収する。
        //
        // 例：
        // ・"draft"       → "DRAFT"
        // ・" confirmed " → "CONFIRMED"
        // ・"unconfirm"   → "UNCONFIRM"
        //
        // Locale.ROOTを指定して、端末やサーバーの言語設定による
        // 大文字変換の差異を避ける。
        final String normalizedAction =
                action == null
                        ? "CONFIRMED"
                        : action
                                .trim()
                                .toUpperCase(
                                        Locale.ROOT
                                );

        // =========================================================
        // 5. 一時保存・確定・確定解除
        // =========================================================

        try {
            // 正常終了時に画面上部へ表示する通知メッセージ。
            String notice;

            switch (normalizedAction) {

            case "DRAFT":

                // ▼ 下書き保存
                //
                // 1セル = 1レコードのまま、
                // status=DRAFTとしてsaveShifts()でupsertする。
                //
                // 既存データがある場合は更新し、
                // 存在しない場合は新規作成する。
                shiftService.saveShifts(

                        form,

                        Shift.Status.DRAFT
                );

                notice =
                        "シフトを一時保存しました。";

                logger.info(
                        "Shift draft saved: department={}, targetMonth={}, "
                                + "submittedShiftCount={}",
                        form.getDepartment(),
                        form.getTargetMonth(),
                        submittedShiftCount
                );

                break;

            case "UNCONFIRM":

                // ▼ 確定解除（CONFIRMED → DRAFT）
                //
                // 対象月・部署のCONFIRMEDをDRAFTに戻す処理は、
                // ShiftService#unconfirmShiftsへ委譲する。
                //
                // 基本的には「確定後に修正したくなったとき」に使用する。
                shiftService.unconfirmShifts(
                        form
                );

                notice =
                        "シフトの確定を解除しました。";

                logger.info(
                        "Shift confirmation removed: department={}, "
                                + "targetMonth={}",
                        form.getDepartment(),
                        form.getTargetMonth()
                );

                break;

            case "CONFIRMED":

                // ▼ 確定保存
                //
                // 画面の入力内容をstatus=CONFIRMEDでupsertする。
                //
                // 同一セルの既存DRAFTを上書きして、
                // 正式なシフトとして扱う。
                shiftService.saveShifts(

                        form,

                        Shift.Status.CONFIRMED
                );

                notice =
                        "シフトを確定しました。";

                logger.info(
                        "Shift confirmed: department={}, targetMonth={}, "
                                + "submittedShiftCount={}",
                        form.getDepartment(),
                        form.getTargetMonth(),
                        submittedShiftCount
                );

                break;

            default:

                // ▼ 想定外のaction
                //
                // 既存コードでは想定外のactionを
                // CONFIRMEDとして保存していた。
                //
                // ただし、不正な値や画面設定ミスによって
                // 意図せず確定保存される可能性があるため、
                // 保存処理は実行せずエラーとして扱う。
                logger.warn(
                        "Unsupported shift save action: department={}, "
                                + "targetMonth={}, action={}",
                        form.getDepartment(),
                        form.getTargetMonth(),
                        normalizedAction
                );

                // 保存されなかった入力済みシフトを保持する。
                preserveSubmittedShifts(
                        form,
                        ra
                );

                ra.addFlashAttribute(
                        "errorMessage",
                        "保存操作を判別できませんでした。"
                                + "もう一度お試しください。"
                );

                return redirectToGenerate(
                        form,
                        ra
                );
            }

            // 画面上部に正常終了の通知を出す。
            //
            // FlashAttributeのため、リダイレクト後の1回だけ
            // generate.htmlで参照できる。
            ra.addFlashAttribute(
                    "notice",
                    notice
            );

        } catch (DataIntegrityViolationException e) {

            // DBの制約違反などにより、
            // シフトの保存・更新に失敗した場合。
            //
            // 技術的な例外内容は画面に表示せず、
            // 調査用にサーバーログへ記録する。
            logger.error(
                    "Database constraint violation during shift save: "
                            + "department={}, targetMonth={}, action={}",
                    form.getDepartment(),
                    form.getTargetMonth(),
                    normalizedAction,
                    e
            );

            // 保存できなかった変更済みシフトを保持する。
            preserveSubmittedShifts(
                    form,
                    ra
            );

            // シフト生成画面へ表示する利用者向けのエラー。
            ra.addFlashAttribute(
                    "errorMessage",
                    "シフトを保存できませんでした。"
                            + "入力内容を確認してから"
                            + "再度お試しください。"
            );

        } catch (IllegalArgumentException e) {

            // Service側で部署・対象年月・勤務区分などの
            // 業務条件に問題があると判断された場合。
            logger.warn(
                    "Invalid shift save parameters: "
                            + "department={}, targetMonth={}, action={}",
                    form.getDepartment(),
                    form.getTargetMonth(),
                    normalizedAction,
                    e
            );

            // 保存できなかった変更済みシフトを保持する。
            preserveSubmittedShifts(
                    form,
                    ra
            );

            ra.addFlashAttribute(
                    "errorMessage",
                    "シフトの入力内容に誤りがあります。"
                            + "部署、対象年月、勤務区分を"
                            + "確認してください。"
            );

        } catch (Exception e) {

            // その他の予期しない保存エラー。
            //
            // 例：
            // ・DB接続エラー
            // ・保存処理中の例外
            // ・確定解除処理中の例外
            logger.error(
                    "Unexpected error during shift save: "
                            + "department={}, targetMonth={}, action={}",
                    form.getDepartment(),
                    form.getTargetMonth(),
                    normalizedAction,
                    e
            );

            // 保存できなかった変更済みシフトを保持する。
            preserveSubmittedShifts(
                    form,
                    ra
            );

            ra.addFlashAttribute(
                    "errorMessage",
                    "シフトの保存に失敗しました。"
                            + "入力内容を保持していますので、"
                            + "確認してから再度お試しください。"
            );
        }

        // =========================================================
        // 6. シフト生成画面へ戻す
        // =========================================================

        // PRG：
        // 保存後、または保存エラー後は、
        // 同じ部署・対象年月の生成画面へ戻る。
        return redirectToGenerate(

                form,

                ra
        );
    }

    /**
     * 保存失敗時の入力済みシフトを保持する。
     *
     * 【保持する値】
     * ShiftGenerationForm#getShifts()で取得できる
     * ユーザーID・日付・勤務区分のMap。
     *
     * 【例】
     *
     * {
     *     "12_2026-08-01": "日",
     *     "12_2026-08-02": "休",
     *     "15_2026-08-01": "夜"
     * }
     *
     * 【画面復元の流れ】
     * 1. 保存エラーが発生する。
     * 2. 変更済みセルをsubmittedShiftsとして保存する。
     * 3. /shift/generateへリダイレクトする。
     * 4. ShiftGenerationControllerがDB上のshiftMapへ
     *    submittedShiftsを重ねて画面へ渡す。
     * 5. 保存されなかった入力内容が画面上に再表示される。
     *
     * 【注意】
     * submittedShiftsはFlashAttributeのため、
     * リダイレクト後の次のリクエストまで一時的に保持される。
     *
     * @param form シフト生成画面から送信されたフォーム
     * @param ra リダイレクト先に引き継ぐ属性
     */
    private void preserveSubmittedShifts(

            ShiftGenerationForm form,

            RedirectAttributes ra
    ) {

        // フォームがない場合や、
        // 入力済みシフトがない場合は保持処理を行わない。
        if (form == null
                || form.getShifts() == null) {

            return;
        }

        // LinkedHashMapへコピーすることで、
        // 元のフォームのMapを直接引き回さず、
        // 入力順を保った状態で次の画面へ渡す。
        ra.addFlashAttribute(

                "submittedShifts",

                new LinkedHashMap<>(
                        form.getShifts()
                )
        );

        logger.debug(
                "Preserved submitted shifts after save failure: "
                        + "department={}, targetMonth={}, count={}",
                form.getDepartment(),
                form.getTargetMonth(),
                form.getShifts().size()
        );
    }

    /**
     * 共通：保存後はgenerate画面にリダイレクトする。
     *
     * 【引き継ぐURLパラメータ】
     * ・department：部署コード。
     * ・month：対象年月。yyyy-MM形式。
     *
     * 【遷移例】
     * /shift/generate?department=amami&month=2026-08
     *
     * 【補足】
     * ・部署が未設定の場合はdepartmentを付与しない。
     * ・対象年月が未設定または変換不可の場合はmonthを付与しない。
     * ・notice、errorMessage、submittedShiftsは
     *   FlashAttributeとして別途引き継ぐ。
     *
     * @param form シフト生成フォーム
     * @param ra リダイレクト先に引き継ぐ属性
     * @return シフト生成画面へのリダイレクト
     */
    private String redirectToGenerate(

            ShiftGenerationForm form,

            RedirectAttributes ra
    ) {

        if (form != null) {

            // 選択済みの部署をURLパラメータとして引き継ぐ。
            if (StringUtils.hasText(
                    form.getDepartment()
            )) {

                ra.addAttribute(

                        "department",

                        form.getDepartment()
                );
            }

            // 対象年月が正常に変換できている場合だけ、
            // yyyy-MM形式でURLパラメータへ設定する。
            if (form.getTargetMonth() != null) {

                ra.addAttribute(

                        "month",

                        form.getTargetMonth().toString()
                );
            }
        }

        // 生成画面の表示処理は
        // ShiftGenerationController#showGeneratePageが担当する。
        return "redirect:/shift/generate";
    }
}