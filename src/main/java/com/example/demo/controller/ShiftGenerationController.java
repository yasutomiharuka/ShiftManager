package com.example.demo.controller;

import java.time.LocalDate;
import java.time.YearMonth;
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
import com.example.demo.service.ShiftService;
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
    private final ShiftService shiftService;

    public ShiftGenerationController(ShiftGenerationService shiftGenerationService,
                                     UserProfileService userProfileService,
                                     ShiftService shiftService) {
        this.shiftGenerationService = shiftGenerationService;
        this.userProfileService = userProfileService;
        this.shiftService = shiftService;
    }

    /**
     * シフト生成画面の表示
     * /shift/generate にGETアクセスされたとき呼ばれる
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

            // --- 2. 月の日付リストを生成（1日〜末日） ---
            List<LocalDate> dates = IntStream.rangeClosed(1, targetMonth.lengthOfMonth())
                    .mapToObj(targetMonth::atDay)
                    .toList();

            // --- 3. 部署でユーザーを絞り込み ---
            List<UserProfileDto> users = userProfileService.getAllUserProfiles().stream()
                    .filter(u -> department.equalsIgnoreCase(u.getDepartment()))
                    .toList();
            System.out.println("▶ ユーザー件数: " + users.size());

            // --- 4. シフト情報を取得 ---
            Map<String, String> shiftMap = shiftService.getShiftMap(users, dates, department);

            // --- 5. 部署コードと日本語表示名のマッピング ---
            Map<String, String> departmentDisplayMap = Map.of(
                    "amami", "天美",
                    "main", "本社"
            );

            // --- 6. Thymeleafに渡す値をmodelにセット ---
            model.addAttribute("users", users);
            model.addAttribute("dates", dates);
            model.addAttribute("department", department);
            model.addAttribute("month", targetMonth);
            model.addAttribute("departments", departmentDisplayMap.keySet());
            model.addAttribute("departmentNames", departmentDisplayMap);
            model.addAttribute("selectedDepartmentName",
                    departmentDisplayMap.getOrDefault(department, department));
            model.addAttribute("shiftMap", shiftMap);
            model.addAttribute("form", new ShiftGenerationForm());

            // 可視化用フラグ
            model.addAttribute("noUsers", users == null || users.isEmpty());
            model.addAttribute("noDates", dates == null || dates.isEmpty());

            // 保険
            if (model.getAttribute("form") == null) {
                model.addAttribute("form", new ShiftGenerationForm());
            }

            System.out.println("▶ モデルへのデータ格納完了");

        } catch (Exception e) {
            System.out.println("❌ エラー発生: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "シフト生成画面の表示中にエラーが発生しました");
            return "error";
        }

        return "shift/generate";
    }
}
