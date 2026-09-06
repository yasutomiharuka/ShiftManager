package com.example.demo.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.form.ShiftRequirementForm;
import com.example.demo.service.ShiftGenerationResult;
import com.example.demo.service.ShiftGenerationService;
import com.example.demo.service.ShiftRequirementService;
import com.example.demo.service.ShiftService;
import com.example.demo.service.UserProfileService;

/**
 * シフト生成に関する画面コントローラ。
 *
 * GET:
 * ・シフト生成画面を表示する。
 * ・対象部署の有効ユーザー情報を取得する。
 * ・保存済みシフト情報を取得する。
 * ・必要人員フォームを取得する。
 *
 * POST:
 * ・対象部署・対象年月のシフトを自動生成する。
 * ・生成結果と警告メッセージを生成画面へ引き継ぐ。
 *
 * ※シフト保存系の処理は、
 *   ShiftEditController（/api/shift/request/...）が担当する。
 *
 * ※必要人員保存系の処理は、
 *   ShiftRequirementControllerが担当する。
 *
 * 【エラー発生時の役割】
 * ・シフト保存に失敗した場合、
 *   対象部署の有効ユーザー・対象月に一致するsubmittedShiftsだけを
 *   既存shiftMapへ重ねる。
 *
 * ・必要人員保存に失敗した場合、
 *   FlashAttributeのrequirementFormをDB取得値より優先する。
 *
 * ・自動生成に失敗した場合、
 *   入力済みシフトを保持して同じ部署・年月の画面へ戻す。
 */
@Controller
@RequestMapping("/shift")
public class ShiftGenerationController {

    // シフト生成画面の表示・生成処理・エラーを記録するLogger。
    private static final Logger logger =
            LoggerFactory.getLogger(
                    ShiftGenerationController.class
            );

    // --- サービスをDI（依存性注入） ---

    // シフトの自動生成を担当するサービス。
    private final ShiftGenerationService shiftGenerationService;

    // 部署に所属するユーザー情報を取得するサービス。
    private final UserProfileService userProfileService;

    // 保存済みシフトの取得を担当するサービス。
    private final ShiftService shiftService;

    // 各日付・時間帯ごとの「必要人員」を扱うサービス。
    //
    // シフト本体（誰が入るか）とは別の集約として管理する。
    private final ShiftRequirementService shiftRequirementService;

    /**
     * シフト生成画面で使用する各サービスを注入する。
     *
     * @param shiftGenerationService シフト自動生成サービス
     * @param userProfileService ユーザー情報取得サービス
     * @param shiftService シフト情報取得サービス
     * @param shiftRequirementService 必要人員取得・保存サービス
     */
    public ShiftGenerationController(

            ShiftGenerationService shiftGenerationService,

            UserProfileService userProfileService,

            ShiftService shiftService,

            ShiftRequirementService shiftRequirementService
    ) {

        this.shiftGenerationService =
                shiftGenerationService;

        this.userProfileService =
                userProfileService;

        this.shiftService =
                shiftService;

        this.shiftRequirementService =
                shiftRequirementService;
    }

    /**
     * シフト生成画面を表示する。
     *
     * /shift/generateへGETアクセスされたときに呼び出される。
     *
     * 【仕様】
     * ・対象月と部署でユーザー・シフトを絞り込む。
     *
     * ・シフトはDRAFT / CONFIRMEDの両方を取得する。
     *
     * ・セル単位で、優先度の高いシフトを表示する。
     *   CONFIRMED > DRAFT。
     *
     * ・必要人員をShiftRequirementServiceから取得し、
     *   ShiftRequirementFormとして画面へ渡す。
     *
     * 【保存エラー後の復元】
     * ・submittedShiftsが存在する場合、
     *   DB取得済みのshiftMapに未保存の入力値を重ねる。
     *
     * ・requirementFormがFlashAttributeに存在する場合、
     *   DBから作成したフォームより入力済みフォームを優先する。
     *
     * @param department 対象部署。未指定の場合はamami。
     * @param month 対象年月。未指定の場合は当月。
     * @param model Thymeleafへ渡す画面表示用データ
     * @return シフト生成画面、または画面表示失敗時はエラー画面
     */
    @GetMapping("/generate")
    public String showGeneratePage(

            // 対象部署。
            //
            // 初期表示では既存仕様に合わせてamamiを使用する。
            @RequestParam(
                    required = false,
                    defaultValue = "amami"
            )
            String department,

            // 対象年月。
            //
            // URL例:
            // /shift/generate?department=amami&month=2026-08
            @RequestParam(
                    required = false
            )
            @DateTimeFormat(
                    pattern = "yyyy-MM"
            )
            YearMonth month,

            Model model
    ) {

        logger.debug(
                "Displaying shift generation page: "
                        + "department={}, month={}",
                department,
                month
        );

        try {

            // =========================================================
            // 1. 対象年月を決定する
            // =========================================================

            // 対象年月が未指定の場合は当月を使用する。
            YearMonth targetMonth =
                    month != null
                            ? month
                            : YearMonth.now();

            logger.debug(
                    "Resolved target month: {}",
                    targetMonth
            );

            // =========================================================
            // 2. 年月を入力用と表示用に分ける
            // =========================================================

            // input type="month"やURLでは、
            // yyyy-MM形式を使用する。
            DateTimeFormatter valueFormatter =
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM"
                    );

            // 画面上部の見出しでは、
            // yyyy年MM月形式を使用する。
            DateTimeFormatter labelFormatter =
                    DateTimeFormatter.ofPattern(
                            "yyyy年MM月"
                    );

            // 例：2026-08。
            String monthValue =
                    targetMonth.format(
                            valueFormatter
                    );

            // 例：2026年08月。
            String monthLabel =
                    targetMonth.format(
                            labelFormatter
                    );

            logger.debug(
                    "Resolved month display values: "
                            + "monthValue={}, monthLabel={}",
                    monthValue,
                    monthLabel
            );

            // =========================================================
            // 3. 対象月の日付一覧を作成する
            // =========================================================

            // 対象月の1日から末日までの日付を作成する。
            //
            // 例：
            // 2026-08-01
            // 2026-08-02
            // ...
            // 2026-08-31
            List<LocalDate> dates =
                    IntStream
                            .rangeClosed(

                                    1,

                                    targetMonth.lengthOfMonth()
                            )
                            .mapToObj(
                                    targetMonth::atDay
                            )
                            .toList();

            // =========================================================
            // 4. 部署に所属するユーザーを取得する
            // =========================================================

            // 有効職員は、シフトの有無にかかわらず表示する。
            List<UserProfileDto> activeUsers =
                    userProfileService
                            .getAllUserProfiles()
                            .stream()
                            .filter(

                                    user -> department.equalsIgnoreCase(
                                            user.getDepartment()
                                    )
                            )
                            .toList();

            Set<Long> activeUserIds = new HashSet<>();
            Map<Long, UserProfileDto> candidateUsers = new LinkedHashMap<>();

            for (UserProfileDto user : activeUsers) {
                activeUserIds.add(user.getId());
                candidateUsers.put(user.getId(), user);
            }

            // 無効職員は現在の所属では絞らず候補へ追加する。
            // 所属変更後に無効化された場合も、保存済みシフトの部署を基準に
            // 対象部署・対象月の履歴を表示できるようにする。
            for (UserProfileDto user : userProfileService.getInactiveUserProfiles()) {
                candidateUsers.putIfAbsent(user.getId(), user);
            }

            logger.debug(
                    "Candidate users loaded for shift generation: "
                            + "department={}, activeCount={}, candidateCount={}",
                    department,
                    activeUsers.size(),
                    candidateUsers.size()
            );

            // =========================================================
            // 5. DBに保存済みのシフトを取得する
            // =========================================================

            // ShiftService#getShiftMapは、
            // DRAFT / CONFIRMEDの両方を読み込む。
            //
            // 同一セルに複数のシフト候補がある場合は、
            // 優先順位と更新日時に従って1件にまとめる。
            Map<String, String> savedShiftMap =
                    candidateUsers.isEmpty()
                            ? Map.of()
                            : shiftService.getShiftMap(
                                    new ArrayList<>(candidateUsers.values()),
                                    dates,
                                    department
                            );

            // 保存エラー後に未保存の入力値を重ねるため、
            // 変更可能なHashMapとして作成する。
            //
            // 念のため、Serviceからnullが返った場合でも
            // 空Mapとして処理できるようにする。
            Map<String, String> shiftMap =
                    savedShiftMap != null
                            ? new HashMap<>(
                                    savedShiftMap
                            )
                            : new HashMap<>();

            // 有効職員は常に表示し、無効職員は対象部署・対象月に
            // 保存済みシフトが存在する場合だけ表示する。
            List<UserProfileDto> users =
                    candidateUsers.values()
                            .stream()
                            .filter(user ->
                                    activeUserIds.contains(user.getId())
                                            || dates.stream().anyMatch(date ->
                                                    shiftMap.containsKey(
                                                            user.getId() + "_" + date
                                                    )
                                            )
                            )
                            .toList();

            Set<Long> inactiveUserIds = new HashSet<>();
            for (UserProfileDto user : users) {
                if (!activeUserIds.contains(user.getId())) {
                    inactiveUserIds.add(user.getId());
                }
            }

            // 画面に表示・復元してよいセルを、最終表示対象から作成する。
            Set<String> editableShiftKeys = new HashSet<>();
            for (UserProfileDto user : users) {
                for (LocalDate date : dates) {
                    editableShiftKeys.add(user.getId() + "_" + date);
                }
            }

            shiftMap.keySet().retainAll(editableShiftKeys);

            // =========================================================
            // 6. 保存失敗時の入力済みシフトを復元する
            // =========================================================

            // ShiftEditControllerまたは本Controllerが、
            // 保存・生成失敗時にFlashAttributeへ設定した
            // submittedShiftsを取得する。
            //
            // 例：
            // {
            //     "12_2026-08-01": "日",
            //     "12_2026-08-02": "休"
            // }
            Object submittedShifts =
                    model
                            .asMap()
                            .get(
                                    "submittedShifts"
                            );

            // 保存されなかった入力済みシフトが存在する場合、
            // DB上の既存シフトより画面上の入力値を優先する。
            if (submittedShifts
                    instanceof Map<?, ?> inputShiftMap) {

                inputShiftMap.forEach(

                        (key, value) -> {

                            // 画面側のシフトMapは、
                            // "ユーザーID_yyyy-MM-dd"形式の
                            // 文字列キーを使用する。
                            if (key instanceof String shiftKey
                                    && editableShiftKeys.contains(shiftKey)) {

                                // 勤務区分がnullの場合は空文字として扱う。
                                //
                                // これにより、削除・クリア操作も
                                // 元の画面に復元できる。
                                shiftMap.put(

                                        shiftKey,

                                        value == null
                                                ? ""
                                                : value.toString()
                                );
                            }
                        }
                );

                logger.debug(
                        "Received submitted shifts; only editable cells restored: "
                                + "department={}, month={}, count={}",
                        department,
                        targetMonth,
                        inputShiftMap.size()
                );
            }

            // =========================================================
            // 7. 部署コードと表示名を設定する
            // =========================================================

            // 部署コードと画面表示名の対応表。
            //
            // amami : 天美
            // main  : 本社
            Map<String, String> departmentDisplayMap =
                    Map.of(

                            "amami",
                            "天美",

                            "main",
                            "本社"
                    );

            // =========================================================
            // 8. シフト入力フォームを作成する
            // =========================================================

            // hidden項目などで、
            // th:field="*{department}"または
            // th:field="*{targetMonth}"を使用する場合に備え、
            // 表示対象の部署と年月をフォームへ設定する。
            ShiftGenerationForm form =
                    new ShiftGenerationForm();

            form.setDepartment(
                    department
            );

            form.setTargetMonth(
                    targetMonth
            );

            // =========================================================
            // 9. 必要人員入力フォームを取得または復元する
            // =========================================================

            // 【ShiftRequirementForm】
            //
            // ・generate.htmlの必要人員入力テーブルで使用する。
            //
            // ・部署、対象年月、日付ごとの時間帯別必要人数を保持する。
            //
            // ・通常表示時はShiftRequirementService#buildFormで
            //   DB上の必要人員から初期値を作成する。
            //
            // ・保存失敗後はFlashAttributeとして保持された
            //   入力済みフォームを優先する。
            ShiftRequirementForm requirementForm;

            // ShiftRequirementControllerが保存失敗時に設定した
            // requirementFormを取得する。
            Object submittedRequirementForm =
                    model
                            .asMap()
                            .get(
                                    "requirementForm"
                            );

            if (submittedRequirementForm
                    instanceof ShiftRequirementForm inputForm) {

                // 保存できなかった入力値を優先する。
                //
                // これを行わずにDBの値だけでフォームを作り直すと、
                // 入力済み必要人数が保存失敗後に消えてしまう。
                requirementForm =
                        inputForm;

                logger.debug(
                        "Restored requirement form after an error: "
                                + "department={}, year={}, month={}",
                        inputForm.getDepartment(),
                        inputForm.getYear(),
                        inputForm.getMonth()
                );

            } else {

                // 通常表示時は、
                // DB上の保存済み必要人員からフォームを作成する。
                requirementForm =
                        shiftRequirementService.buildForm(

                                department,

                                targetMonth
                        );
            }

            // =========================================================
            // 10. Thymeleafへ画面表示データを渡す
            // =========================================================

            // 部署に所属する有効ユーザー一覧。
            model.addAttribute(

                    "users",

                    users
            );

            // 対象月の日付一覧。
            model.addAttribute(

                    "dates",

                    dates
            );

            // 選択中の部署コード。
            model.addAttribute(

                    "department",

                    department
            );

            // monthは用途によって形式が異なるため、
            // 変数名を分けて画面へ渡す。

            // Java側のロジックで使用するYearMonth。
            model.addAttribute(

                    "targetMonth",

                    targetMonth
            );

            // input type="month"およびURL用のyyyy-MM形式。
            model.addAttribute(

                    "monthValue",

                    monthValue
            );

            // 画面見出し用のyyyy年MM月形式。
            model.addAttribute(

                    "monthLabel",

                    monthLabel
            );

            // 部署選択フォームで使用する部署コード一覧。
            model.addAttribute(

                    "departments",

                    departmentDisplayMap.keySet()
            );

            // 部署コードと表示名の対応表。
            model.addAttribute(

                    "departmentNames",

                    departmentDisplayMap
            );

            // 現在選択中の部署名。
            //
            // 未定義の部署コードの場合は、
            // 念のためコードをそのまま表示する。
            model.addAttribute(

                    "selectedDepartmentName",

                    departmentDisplayMap.getOrDefault(

                            department,

                            department
                    )
            );

            // DB上の保存済みシフトに、
            // 保存失敗時の入力値を重ねた表示用Map。
            model.addAttribute(

                    "shiftMap",

                    shiftMap
            );

            // generate.htmlで無効職員を識別し、氏名横へ「無効」と表示する。
            model.addAttribute(

                    "inactiveUserIds",

                    inactiveUserIds
            );

            // 過去月は履歴の表示・手動訂正だけを許可し、自動生成は行わない。
            model.addAttribute(

                    "pastMonth",

                    targetMonth.isBefore(YearMonth.now())
            );

            // シフト入力フォーム。
            model.addAttribute(

                    "form",

                    form
            );

            // 必要人員入力フォーム。
            //
            // 保存失敗後は、DB上の値ではなく、
            // 直前の入力内容を保持したフォームが設定される。
            model.addAttribute(

                    "requirementForm",

                    requirementForm
            );

            // 画面表示で使用する補助フラグ。
            model.addAttribute(

                    "noUsers",

                    users == null
                            || users.isEmpty()
            );

            model.addAttribute(

                    "noDates",

                    dates == null
                            || dates.isEmpty()
            );

            logger.debug(
                    "Shift generation page model prepared: "
                            + "department={}, month={}, "
                            + "userCount={}, shiftCount={}",
                    department,
                    targetMonth,
                    users.size(),
                    shiftMap.size()
            );

        } catch (Exception e) {

            // 画面表示に必要なユーザー・シフト・必要人員を
            // 取得できなかった場合。
            //
            // 例外の技術的な詳細はサーバーログへ記録し、
            // 画面には利用者向けメッセージだけを渡す。
            logger.error(
                    "Failed to display shift generation page: "
                            + "department={}, month={}",
                    department,
                    month,
                    e
            );

            model.addAttribute(

                    "errorMessage",

                    "シフト生成画面の表示中に"
                            + "エラーが発生しました。"
            );

            // 既存仕様どおり、
            // 画面表示自体に失敗した場合は共通エラー画面を表示する。
            return "error";
        }

        // シフト生成画面のテンプレートを表示する。
        return "shift/generate";
    }

    /**
     * シフト自動生成を実行する。
     *
     * 【呼び出し元】
     * shift/generate.htmlの
     * 「シフトを生成する」ボタン。
     *
     * 【処理概要】
     * 1. 対象部署・対象年月を確認する。
     * 2. 有効職員の対象部署・対象月のAUTOだけを再生成対象とする。
     * 3. MANUALと無効職員の既存シフトは保持する。
     * 4. シフト自動生成処理を実行する。
     * 5. 同じ部署・年月の生成画面へリダイレクトする。
     *
     * 【生成エラー時】
     * ・入力済みシフトをsubmittedShiftsとして保持する。
     * ・errorMessageをFlashAttributeへ設定する。
     * ・同じ部署・対象年月の生成画面へ戻す。
     *
     * @param form シフト生成画面から送信されたフォーム
     * @param bindingResult フォームの変換・バインドエラー情報
     * @param department 対象部署
     * @param targetMonth 対象年月。yyyy-MM形式。
     * @param redirectAttributes リダイレクト時の引き継ぎ情報
     * @return シフト生成画面へのリダイレクト
     */
    @PostMapping("/generate")
    public String generateShift(

            // 変更済みシフトを受け取るため、
            // 既存の部署・対象年月に加えてフォーム全体を受け取る。
            @ModelAttribute
            ShiftGenerationForm form,

            // targetMonthなどの変換エラーを受け取る。
            //
            // BindingResultは対象フォーム引数の直後に配置する。
            BindingResult bindingResult,

            // 対象部署。
            //
            // required=falseとすることで、
            // 未指定時もController内でエラー処理できる。
            @RequestParam(
                    required = false
            )
            String department,

            // 対象年月。
            //
            // required=falseとすることで、
            // 未指定時も元の画面へ戻す処理を実行できる。
            @RequestParam(
                    required = false
            )
            String targetMonth,

            RedirectAttributes redirectAttributes
    ) {

        logger.debug(
                "Starting automatic shift generation: "
                        + "department={}, targetMonth={}",
                department,
                targetMonth
        );

        // =========================================================
        // 1. 対象部署を確認する
        // =========================================================

        if (!StringUtils.hasText(
                department
        )) {

            logger.warn(
                    "Shift generation failed because department is missing."
            );

            // 自動生成前に入力されていたシフトを保持する。
            preserveSubmittedShifts(

                    form,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "部署が指定されていません。"
                            + "部署を選択してから"
                            + "シフトを生成してください。"
            );

            return redirectToGenerate(

                    form,

                    department,

                    null,

                    redirectAttributes
            );
        }

        // 前後の空白がある場合に備え、
        // 生成処理前に部署コードを正規化する。
        String resolvedDepartment =
                department.trim();

        // =========================================================
        // 2. フォームの変換エラーを確認する
        // =========================================================

        if (bindingResult.hasErrors()) {

            logger.warn(
                    "Shift generation form binding errors: "
                            + "department={}, fields={}",
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

            preserveSubmittedShifts(

                    form,

                    redirectAttributes
            );

            // 対象年月の変換に失敗した場合は、
            // 原因がわかるメッセージを表示する。
            if (bindingResult.hasFieldErrors(
                    "targetMonth"
            )) {

                redirectAttributes.addFlashAttribute(

                        "errorMessage",

                        "対象年月を正しく指定してください。"
                );

            } else {

                redirectAttributes.addFlashAttribute(

                        "errorMessage",

                        "シフトの入力内容に誤りがあります。"
                                + "内容をご確認ください。"
                );
            }

            // 対象年月が不正な場合は、
            // monthパラメータへ不正値をそのまま渡さない。
            return redirectToGenerate(

                    form,

                    resolvedDepartment,

                    null,

                    redirectAttributes
            );
        }

        // =========================================================
        // 3. 対象年月を確認する
        // =========================================================

        if (!StringUtils.hasText(
                targetMonth
        )) {

            logger.warn(
                    "Shift generation failed because target month is missing: "
                            + "department={}",
                    resolvedDepartment
            );

            preserveSubmittedShifts(

                    form,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "対象年月が指定されていません。"
                            + "対象年月を選択してから"
                            + "シフトを生成してください。"
            );

            return redirectToGenerate(

                    form,

                    resolvedDepartment,

                    null,

                    redirectAttributes
            );
        }

        // 入力された対象年月がyyyy-MM形式であり、
        // 実際に存在する年月であることを確認する。
        //
        // 例：
        // ・2026-08 → 正常。
        // ・2026-13 → エラー。
        // ・2026/08 → エラー。
        String normalizedTargetMonth;

        try {

            normalizedTargetMonth =
                    YearMonth
                            .parse(
                                    targetMonth.trim()
                            )
                            .toString();

        } catch (DateTimeParseException e) {

            logger.warn(
                    "Invalid target month for shift generation: "
                            + "department={}, targetMonth={}",
                    resolvedDepartment,
                    targetMonth
            );

            preserveSubmittedShifts(

                    form,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "対象年月を正しく指定してください。"
            );

            // 不正な年月をmonthパラメータへ引き継ぐと、
            // GET側で再度変換エラーになるため設定しない。
            return redirectToGenerate(

                    form,

                    resolvedDepartment,

                    null,

                    redirectAttributes
            );
        }

        YearMonth resolvedTargetMonth =
                YearMonth.parse(normalizedTargetMonth);

        // 過去月の自動生成は、画面上のボタン制御だけに依存せず
        // Controllerでも拒否して既存シフトを保護する。
        if (resolvedTargetMonth.isBefore(YearMonth.now())) {

            logger.warn(
                    "Past month shift generation was rejected: "
                            + "department={}, targetMonth={}",
                    resolvedDepartment,
                    normalizedTargetMonth
            );

            preserveSubmittedShifts(

                    form,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "過去月のシフトは自動生成できません。"
                            + "必要な修正は手動で行ってください。"
            );

            return redirectToGenerate(

                    form,

                    resolvedDepartment,

                    normalizedTargetMonth,

                    redirectAttributes
            );
        }

        // =========================================================
        // 4. シフト自動生成処理を実行する
        // =========================================================

        try {

            // 修正済みServiceで有効職員だけを自動生成する。
            // 有効職員のAUTOだけを削除・再生成し、
            // MANUALと無効職員の既存シフトはそのまま保持する。
            // 有効職員が0人の場合はServiceが削除・保存せず警告を返す。
            //
            // ShiftGenerationService側の@Transactionalにより、
            // 処理途中で例外が発生した場合はロールバックされる。
            ShiftGenerationResult result =
                    shiftGenerationService
                            .generateComplementShift(

                                    resolvedDepartment,

                                    normalizedTargetMonth
                            );

            // 生成成功時に画面へ表示する通知。
            redirectAttributes.addFlashAttribute(

                    "notice",

                    "シフト生成処理を実行しました。"
            );

            // 必要人員不足などの警告がある場合、
            // シフト生成画面に引き継ぐ。
            redirectAttributes.addFlashAttribute(

                    "warnings",

                    result.getWarnings()
            );

            logger.info(
                    "Automatic shift generation completed: "
                            + "department={}, targetMonth={}",
                    resolvedDepartment,
                    normalizedTargetMonth
            );

        } catch (IllegalArgumentException e) {

            // 部署・対象年月・生成条件などに問題がある場合。
            logger.warn(
                    "Invalid shift generation conditions: "
                            + "department={}, targetMonth={}",
                    resolvedDepartment,
                    normalizedTargetMonth,
                    e
            );

            // 保存されなかった入力済みシフトを保持する。
            preserveSubmittedShifts(

                    form,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "シフトを生成できませんでした。"
                            + "部署、対象年月、必要人員を"
                            + "ご確認ください。"
            );

        } catch (Exception e) {

            // DB更新エラーなど、
            // その他の予期しない自動生成エラー。
            logger.error(
                    "Automatic shift generation failed: "
                            + "department={}, targetMonth={}",
                    resolvedDepartment,
                    normalizedTargetMonth,
                    e
            );

            // 自動生成前に入力されていた変更済みシフトを保持する。
            preserveSubmittedShifts(

                    form,

                    redirectAttributes
            );

            redirectAttributes.addFlashAttribute(

                    "errorMessage",

                    "シフトを生成できませんでした。"
                            + "必要人員や勤務条件を確認してから"
                            + "再度お試しください。"
            );
        }

        // =========================================================
        // 5. 同じ部署・対象年月の生成画面へ戻す
        // =========================================================

        return redirectToGenerate(

                form,

                resolvedDepartment,

                normalizedTargetMonth,

                redirectAttributes
        );
    }

    /**
     * 自動生成失敗時の入力済みシフトを保持する。
     *
     * 【保持する値】
     * ShiftGenerationForm#getShifts()で取得する
     * ユーザーID・日付・勤務区分のMap。
     *
     * 【例】
     *
     * {
     *     "12_2026-08-01": "日",
     *     "12_2026-08-02": "休"
     * }
     *
     * 【復元の流れ】
     * 1. 自動生成に失敗する。
     * 2. submittedShiftsをFlashAttributeへ設定する。
     * 3. /shift/generateへリダイレクトする。
     * 4. showGeneratePageが有効職員・対象月の入力値だけを表示用shiftMapへ重ねる。
     * 5. 生成前に入力されていた勤務区分が再表示される。
     *
     * @param form シフト生成画面から送信されたフォーム
     * @param redirectAttributes リダイレクト先へ渡す属性
     */
    private void preserveSubmittedShifts(

            ShiftGenerationForm form,

            RedirectAttributes redirectAttributes
    ) {

        // フォームまたは入力済みシフトがない場合は、
        // 保持対象がないため処理しない。
        if (form == null
                || form.getShifts() == null) {

            return;
        }

        // 元のMapを直接引き回さず、
        // 入力順を保ったコピーとして次の画面へ渡す。
        // 表示時に最新の有効職員一覧と照合し、対象外のセルを除外する。
        // これは画面復元用であり、保存APIの無効職員チェックを代替しない。
        redirectAttributes.addFlashAttribute(

                "submittedShifts",

                new LinkedHashMap<>(
                        form.getShifts()
                )
        );

        logger.debug(
                "Preserved submitted shifts: "
                        + "department={}, targetMonth={}, count={}",
                form.getDepartment(),
                form.getTargetMonth(),
                form.getShifts().size()
        );
    }

    /**
     * シフト生成画面へリダイレクトする。
     *
     * 【引き継ぐURLパラメータ】
     * ・department：対象部署。
     * ・month：対象年月。yyyy-MM形式。
     *
     * 【遷移例】
     * /shift/generate?department=amami&month=2026-08
     *
     * 【注意】
     * ・対象年月が不正な場合はmonthを引き継がない。
     * ・notice、warnings、errorMessage、submittedShiftsは、
     *   FlashAttributeとして別途引き継ぐ。
     *
     * @param form シフト生成フォーム
     * @param department 対象部署
     * @param targetMonth 有効な対象年月
     * @param redirectAttributes リダイレクト先へ渡す属性
     * @return シフト生成画面へのリダイレクト
     */
    private String redirectToGenerate(

            ShiftGenerationForm form,

            String department,

            String targetMonth,

            RedirectAttributes redirectAttributes
    ) {

        // 引数の部署を優先して引き継ぐ。
        if (StringUtils.hasText(
                department
        )) {

            redirectAttributes.addAttribute(

                    "department",

                    department.trim()
            );

        } else if (

                form != null

                        && StringUtils.hasText(
                                form.getDepartment()
                        )
        ) {

            // 引数に部署がない場合は、
            // フォームに設定された部署を保険として使用する。
            redirectAttributes.addAttribute(

                    "department",

                    form.getDepartment().trim()
            );
        }

        // 正常に解析できた対象年月だけをURLへ設定する。
        //
        // 不正な年月を設定すると、
        // GET /shift/generate側で再度変換エラーになるため、
        // nullの場合はmonthパラメータを付与しない。
        if (StringUtils.hasText(
                targetMonth
        )) {

            redirectAttributes.addAttribute(

                    "month",

                    targetMonth
            );
        }

        return "redirect:/shift/generate";
    }
}
