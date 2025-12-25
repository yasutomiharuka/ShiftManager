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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.service.ShiftService;
import com.example.demo.service.UserProfileService;

@Controller
@RequestMapping("/shift")
public class ShiftListController {

    private final ShiftService shiftService;
    private final UserProfileService userProfileService;

    public ShiftListController(ShiftService shiftService, UserProfileService userProfileService) {
        this.shiftService = shiftService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/list")
    public String showShiftList(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false, defaultValue = "amami") String department,
            Model model) {

        // ① 対象月を決定（指定がなければ今月）
        YearMonth targetMonth = (month != null) ? month : YearMonth.now();

        // ② month を「送信用(yyyy-MM)」と「表示用(yyyy年MM月)」に分ける（テンプレ衝突回避＆実務向け）
        DateTimeFormatter VALUE_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("yyyy年MM月");
        String monthValue = targetMonth.format(VALUE_FMT); // 例: 2025-12（input/URL用）
        String monthLabel = targetMonth.format(LABEL_FMT); // 例: 2025年12月（見出し表示用）

        // ③ 指定月の全日付をリスト化（1日〜末日）
        List<LocalDate> dates = IntStream.rangeClosed(1, targetMonth.lengthOfMonth())
                .mapToObj(targetMonth::atDay)
                .toList();

        // ④ ユーザー一覧を部署コード（amami/main）でフィルタリング
        List<UserProfileDto> users = userProfileService.getAllUserProfiles().stream()
                .filter(u -> department.equalsIgnoreCase(u.getDepartment()))
                .toList();

        // ⑤ シフト情報を userId + '_' + 日付 をキーとして取得（表示用）
        Map<String, String> shiftMap = shiftService.getShiftMap(users, dates, department);

        // ⑥ 部署コードと表示名のマッピング（日本語表示用）
        Map<String, String> departmentDisplayMap = Map.of(
                "amami", "天美",
                "main", "本社"
        );

        // ⑦ モデルに各種属性を追加
        model.addAttribute("users", users);
        model.addAttribute("dates", dates);
        model.addAttribute("shiftMap", shiftMap);
        model.addAttribute("department", department);

        // ★month は衝突しやすいので、用途別に分けて渡す
        // model.addAttribute("month", targetMonth);
        model.addAttribute("monthValue", monthValue); // input type="month" / URL用（yyyy-MM）
        model.addAttribute("monthLabel", monthLabel); // 見出し表示用（yyyy年MM月）

        model.addAttribute("departments", departmentDisplayMap.keySet()); // セレクト用
        model.addAttribute("departmentNames", departmentDisplayMap); // 表示名用
        model.addAttribute("selectedDepartmentName", departmentDisplayMap.getOrDefault(department, department));

        return "shift/list";
    }
}
