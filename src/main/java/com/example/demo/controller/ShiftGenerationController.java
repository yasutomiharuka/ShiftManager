package com.example.demo.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.service.ShiftGenerationService;
import com.example.demo.service.UserProfileService;

/**
 * シフト生成に関する画面コントローラ
 * GET: 画面表示
 * POST: 一時保存、シフト生成処理など
 */
@Controller
@RequestMapping("/shift")
public class ShiftGenerationController {

    // --- サービスをDI（依存性注入） ---
    private final ShiftGenerationService shiftGenerationService;
    private final UserProfileService userProfileService;

    public ShiftGenerationController(ShiftGenerationService shiftGenerationService,
                                     UserProfileService userProfileService) {
        this.shiftGenerationService = shiftGenerationService;
        this.userProfileService = userProfileService;
    }

    /**
     * シフト生成画面の表示
     * /shift/generate にGETアクセスされたとき呼ばれる
     */
    @GetMapping("/generate")
    public String showGeneratePage(
            @RequestParam(required = false, defaultValue = "amami") String department,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, // URLパラメータ ?month=2025-08 を受け取れる
            Model model) {

        System.out.println("▶ showGeneratePage 開始");

        try {
            // --- 1. 対象月を決定（指定なければ今月） ---
            YearMonth targetMonth = (month != null) ? month : YearMonth.now();
            System.out.println("▶ 対象月: " + targetMonth);

            // --- 2. 月の日付リストを生成（1日〜末日） ---
            List<LocalDate> dates = IntStream.rangeClosed(1, targetMonth.lengthOfMonth())
                    .mapToObj(targetMonth::atDay)
                    .toList();

            // --- 3. 部署でユーザーを絞り込み ---
            List<UserProfileDto> users = userProfileService.getAllUserProfiles().stream()
                    .filter(u -> department.equalsIgnoreCase(u.getDepartment()))
                    .toList();
            System.out.println("▶ ユーザー件数: " + users.size());

            // --- 4. 部署コードと日本語表示名のマッピング ---
            Map<String, String> departmentDisplayMap = Map.of(
                    "amami", "天美",
                    "main", "本社"
            );

            // ★ 追加：テンプレで参照する shiftMap は必ず non-null に
            Map<String, String> shiftMap = new HashMap<>();
            // 既存シフトを表示したい場合は、必要になったらサービスを呼び出して上書きする
            // shiftMap = shiftService.buildShiftMap(department, targetMonth);

            // --- 5. Thymeleafに渡す値をmodelにセット ---
            model.addAttribute("users", users);                     // 表示対象のユーザー
            model.addAttribute("dates", dates);                     // 日付リスト
            model.addAttribute("department", department);           // 部署コード
            model.addAttribute("month", targetMonth);               // 対象月
            model.addAttribute("departments", departmentDisplayMap.keySet()); // セレクトボックス用
            model.addAttribute("departmentNames", departmentDisplayMap);      // 表示名用
            model.addAttribute("selectedDepartmentName",
                    departmentDisplayMap.getOrDefault(department, department));
            model.addAttribute("shiftMap", shiftMap); // shiftMap
            model.addAttribute("form", new ShiftGenerationForm());  // 入力フォーム用オブジェクト

            // 一覧/日付が空の場合の可視化用フラグ（必ず boolean を渡す）
            boolean noUsers = (users == null || users.isEmpty());
            boolean noDates = (dates == null || dates.isEmpty());
            model.addAttribute("noUsers", noUsers);
            model.addAttribute("noDates", noDates);

            // shiftMap をまだ使うテンプレートがある場合の NPE/SpEL 回避（空マップを渡す）
            model.addAttribute("shiftMap", new java.util.HashMap<String, String>());

            // form が null にならないように保険
            if (model.getAttribute("form") == null) {
                model.addAttribute("form", new ShiftGenerationForm());
            }

            System.out.println("▶ モデルへのデータ格納完了");

        } catch (Exception e) {
            // 例外発生時はエラーメッセージを表示
            System.out.println("❌ エラー発生: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "シフト生成画面の表示中にエラーが発生しました");
            return "error"; // error.html を用意していない場合は list/generate にリダイレクトでもOK
        }

        return "shift/generate"; // 表示するThymeleafテンプレート名
    }
}
