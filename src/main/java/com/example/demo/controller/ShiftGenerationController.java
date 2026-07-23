package com.example.demo.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.form.ShiftRequirementForm;          // ★必要人員入力フォーム
import com.example.demo.service.ShiftGenerationResult;
import com.example.demo.service.ShiftGenerationService;
import com.example.demo.service.ShiftRequirementService;    // ★必要人員サービス
import com.example.demo.service.ShiftService;
import com.example.demo.service.StaffingBalanceService;
import com.example.demo.service.UserProfileService;

/**
 * シフト生成に関する画面コントローラ
 * GET: 画面表示
 * POST: 一時保存、シフト生成処理など
 *
 * ※保存系の処理は ShiftEditController（/api/shift/request/...）に集約。
 *   本コントローラは「表示専用」として、
 *   シフト表示用のデータ（shiftMap など）をモデルに詰める役割。
 */
@Controller
@RequestMapping("/shift")
public class ShiftGenerationController {

    // --- サービスをDI（依存性注入） ---
    private final ShiftGenerationService shiftGenerationService;
    private final UserProfileService userProfileService;
    private final ShiftService shiftService;
    private final StaffingBalanceService staffingBalanceService;

    // ★追加：各日付・時間帯ごとの「必要人員」を扱うサービス
    //   シフト本体（誰が入るか）とは別集約として管理する。
    private final ShiftRequirementService shiftRequirementService;

    public ShiftGenerationController(ShiftGenerationService shiftGenerationService,
                                     UserProfileService userProfileService,
                                     ShiftService shiftService,
                                     ShiftRequirementService shiftRequirementService,
                                     StaffingBalanceService staffingBalanceService) { // ★引数追加
        this.shiftGenerationService = shiftGenerationService;
        this.userProfileService = userProfileService;
        this.shiftService = shiftService;
        this.shiftRequirementService = shiftRequirementService; // ★フィールドに設定
        this.staffingBalanceService = staffingBalanceService;
    }

    /**
     * シフト生成画面の表示
     * /shift/generate にGETアクセスされたとき呼ばれる
     *
     * 仕様：
     *  - 対象月と部署でユーザー・シフトを絞り込み
     *  - シフトは DRAFT / CONFIRMED の両方を取得し、
     *    「セル単位で最新 or 優先度の高いもの（CONFIRMED > DRAFT）」を
     *    ShiftService#getShiftMap でマージして表示に使用する
     *
     *  - 加えて、必要人員（部署×日付×時間帯ごとの人数）を
     *    ShiftRequirementService から取得し、必要人員入力フォーム
     *    （ShiftRequirementForm）としてモデルに渡す。
     */
    @GetMapping("/generate")
    public String showGeneratePage(
            @RequestParam(required = false, defaultValue = "amami") String department,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            Model model) {

        System.out.println("▶ showGeneratePage 開始");

        try {
            // --- 1. 対象月を決定（指定なければ今月） ---
            YearMonth targetMonth = (month != null) ? month : YearMonth.now();
            System.out.println("▶ 対象月: " + targetMonth);

            // --- 1.5. month を「入力・URL用」と「表示用」に分ける（実務でよくやる） ---
            // input type="month" は "yyyy-MM" の形式が必要
            DateTimeFormatter VALUE_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
            // 見出しなど表示は "yyyy年MM月" が読みやすい
            DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("yyyy年MM月");

            String monthValue = targetMonth.format(VALUE_FMT); // 例: 2025-12
            String monthLabel = targetMonth.format(LABEL_FMT); // 例: 2025年12月
            System.out.println("▶ monthValue: " + monthValue + " / monthLabel: " + monthLabel);

            // --- 2. 月の日付リストを生成（1日〜末日） ---
            List<LocalDate> dates = IntStream.rangeClosed(1, targetMonth.lengthOfMonth())
                    .mapToObj(targetMonth::atDay)
                    .toList();

            // --- 3. 部署でユーザーを絞り込み ---
            //   UserProfileService から全ユーザー取得 → department でフィルタ
            List<UserProfileDto> users = userProfileService.getAllUserProfiles().stream()
                    .filter(u -> department.equalsIgnoreCase(u.getDepartment()))
                    .toList();
            System.out.println("▶ ユーザー件数: " + users.size());

            // --- 4. シフト情報を取得 ---
            //   ShiftService#getShiftMap は DRAFT / CONFIRMED 両方を読み込み、
            //   セル単位で優先度（CONFIRMED > DRAFT）と updatedAt に従って1件にマージする
            Map<String, String> shiftMap = shiftService.getShiftMap(users, dates, department);
            Map<String, Integer> staffingBalanceMap =
                    staffingBalanceService.buildStaffingBalanceMap(department, targetMonth);

            // --- 5. 部署コードと日本語表示名のマッピング ---
            Map<String, String> departmentDisplayMap = Map.of(
                    "amami", "天美",
                    "main", "本社"
            );

            // --- 6. 画面バインド用フォームを生成 ---
            // hidden項目などで th:field="*{department}" / "*{targetMonth}" を使う場合を想定し、
            // ここでフォームにも設定しておく
            ShiftGenerationForm form = new ShiftGenerationForm();
            form.setDepartment(department);
            form.setTargetMonth(targetMonth);

            // --- 6.5 必要人員入力フォームの生成 ---
            //   【ShiftRequirementForm】
            //   ・シフト生成画面（generate.html）の「必要人員」入力テーブル用フォーム。
            //   ・1フォーム = ある部署＋対象月の「日付 × 時間帯ごとの必要人数」を保持する。
            //   ・ShiftRequirementService#buildForm(...) で
            //     ShiftRequirement エンティティから初期値を組み立てる。
            ShiftRequirementForm requirementForm =
                    shiftRequirementService.buildForm(department, targetMonth);

            // --- 7. Thymeleafに渡す値をmodelにセット ---
            model.addAttribute("users", users);
            model.addAttribute("dates", dates);
            model.addAttribute("department", department);

            // ★month は衝突しやすい名前なので、用途別に分けて渡す
            // model.addAttribute("month", targetMonth);
            model.addAttribute("targetMonth", targetMonth); // 必要ならロジック用（YearMonth）
            model.addAttribute("monthValue", monthValue);   // input/URL用（yyyy-MM）
            model.addAttribute("monthLabel", monthLabel);   // 見出し表示用（yyyy年MM月）

            model.addAttribute("departments", departmentDisplayMap.keySet());
            model.addAttribute("departmentNames", departmentDisplayMap);
            model.addAttribute("selectedDepartmentName",
                    departmentDisplayMap.getOrDefault(department, department));
            model.addAttribute("shiftMap", shiftMap);
            model.addAttribute("staffingBalanceMap", staffingBalanceMap);
            model.addAttribute("form", form);

            // ★必要人員入力フォーム（別フォーム）を画面へ渡す
            //   generate.html 側では th:object="${requirementForm}" として
            //   「必要人員を保存」ボタン用の <form> から利用する想定。
            model.addAttribute("requirementForm", requirementForm);

            // 可視化用フラグ
            model.addAttribute("noUsers", users == null || users.isEmpty());
            model.addAttribute("noDates", dates == null || dates.isEmpty());

            System.out.println("▶ モデルへのデータ格納完了");

        } catch (Exception e) {
            System.out.println("❌ エラー発生: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "シフト生成画面の表示中にエラーが発生しました");
            return "error";
        }

        return "shift/generate";
    }
    
    /**
     * シフト自動生成を実行する。
     *
     * 【呼び出し元】
     * シフト生成画面（shift/generate.html）
     * 「シフトを生成する」ボタン
     *
     * 【処理概要】
     * 1. 自動生成済みシフト（AUTO）を削除
     * 2. 手入力シフト（MANUAL）は保持
     * 3. シフト自動生成処理を実行
     * 4. シフト生成画面へリダイレクト
     *
     * @param department 対象部署
     * @param targetMonth 対象年月（yyyy-MM）
     * @param redirectAttributes リダイレクト時のメッセージ格納先
     * @return シフト生成画面へリダイレクト
     */
    @PostMapping("/generate")
    public String generateShift(
            @RequestParam String department,
            @RequestParam String targetMonth,
            RedirectAttributes redirectAttributes) {

        ShiftGenerationResult result =
                shiftGenerationService.generateComplementShift(department, targetMonth);

        redirectAttributes.addFlashAttribute("notice", "シフト生成処理を実行しました。");
        redirectAttributes.addFlashAttribute("warnings", result.getWarnings());

        return "redirect:/shift/generate?department=" + department + "&month=" + targetMonth;
    }
}
