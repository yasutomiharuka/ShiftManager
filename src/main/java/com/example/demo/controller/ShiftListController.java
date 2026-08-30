package com.example.demo.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.service.ShiftService;
import com.example.demo.service.StaffingBalanceService;
import com.example.demo.service.UserProfileService;

/**
 * シフト一覧を表示するController。
 *
 * 【表示対象】
 * ・選択部署に所属する有効職員
 * ・選択部署・対象月に保存済みシフトがある無効職員
 *
 * 無効職員の表示は履歴確認のためであり、
 * 新しいシフトの入力・生成を許可するものではない。
 */
@Controller
@RequestMapping("/shift")
public class ShiftListController {

    private final ShiftService shiftService;
    private final UserProfileService userProfileService;
    private final StaffingBalanceService staffingBalanceService;

    public ShiftListController(
            ShiftService shiftService,
            UserProfileService userProfileService,
            StaffingBalanceService staffingBalanceService
    ) {
        this.shiftService = shiftService;
        this.userProfileService = userProfileService;
        this.staffingBalanceService = staffingBalanceService;
    }

    @GetMapping("/list")
    public String showShiftList(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth month,

            @RequestParam(
                    required = false,
                    defaultValue = "amami"
            )
            String department,

            Model model
    ) {

        // ① 対象月を決定する。未指定の場合は当月。
        YearMonth targetMonth =
                month != null ? month : YearMonth.now();

        // ② 年月を送信用と表示用に分ける。
        DateTimeFormatter valueFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM");

        DateTimeFormatter labelFormatter =
                DateTimeFormatter.ofPattern("yyyy年MM月");

        String monthValue =
                targetMonth.format(valueFormatter);

        String monthLabel =
                targetMonth.format(labelFormatter);

        // ③ 対象月の全日付を作成する。
        List<LocalDate> dates = IntStream
                .rangeClosed(1, targetMonth.lengthOfMonth())
                .mapToObj(targetMonth::atDay)
                .toList();

        // ④ 選択部署の有効職員を取得する。
        //
        // 前回修正したgetAllUserProfiles()は、
        // active=trueのユーザーだけを返す。
        List<UserProfileDto> activeUsers =
                userProfileService.getAllUserProfiles()
                        .stream()
                        .filter(user ->
                                department.equalsIgnoreCase(
                                        user.getDepartment()
                                )
                        )
                        .toList();

        // 有効職員は、シフト未登録でも一覧に表示する。
        Set<Long> activeUserIds = new HashSet<>();

        // IDをキーにして、同じ職員の重複表示を防ぐ。
        Map<Long, UserProfileDto> candidateUsers =
                new LinkedHashMap<>();

        for (UserProfileDto user : activeUsers) {
            activeUserIds.add(user.getId());
            candidateUsers.put(user.getId(), user);
        }

        // ⑤ 履歴確認用に無効職員を候補へ追加する。
        //
        // 無効職員は現在の所属だけでは絞らない。
        // 所属変更後に無効化された職員についても、
        // 保存済みシフトの部署を基準に履歴を取得するため。
        for (UserProfileDto user :
                userProfileService.getInactiveUserProfiles()) {

            candidateUsers.putIfAbsent(user.getId(), user);
        }

        // ⑥ 対象部署・対象月の保存済みシフトを取得する。
        //
        // 既存Serviceの優先順位
        // （CONFIRMED > DRAFT）による表示は維持する。
        Map<String, String> savedShiftMap =
                candidateUsers.isEmpty()
                        ? Map.of()
                        : shiftService.getShiftMap(
                                new ArrayList<>(
                                        candidateUsers.values()
                                ),
                                dates,
                                department
                        );

        // 表示用のコピー。DB上のシフトは変更しない。
        Map<String, String> shiftMap =
                savedShiftMap != null
                        ? new HashMap<>(savedShiftMap)
                        : new HashMap<>();

        // 対象職員・対象月に一致するキーだけを画面へ渡す。
        Set<String> allowedShiftKeys = new HashSet<>();

        for (Long userId : candidateUsers.keySet()) {
            for (LocalDate date : dates) {
                allowedShiftKeys.add(userId + "_" + date);
            }
        }

        shiftMap.keySet().retainAll(allowedShiftKeys);

        // ⑦ 最終的な表示対象を決定する。
        //
        // ・有効職員：シフトの有無にかかわらず表示
        // ・無効職員：対象部署・月に表示対象シフトがある場合だけ表示
        List<UserProfileDto> users =
                candidateUsers.values()
                        .stream()
                        .filter(user ->
                                activeUserIds.contains(user.getId())
                                        || dates.stream().anyMatch(
                                                date -> shiftMap.containsKey(
                                                        user.getId()
                                                                + "_"
                                                                + date
                                                )
                                        )
                        )
                        .toList();

        // ⑧ 必要人員との差分を取得する。
        //
        // 集計処理は既存Serviceを維持する。
        // 履歴を含む集計対象の変更は、このControllerでは行わない。
        Map<String, Integer> staffingBalanceMap =
                staffingBalanceService.buildStaffingBalanceMap(
                        department,
                        targetMonth
                );

        // ⑨ 部署コードと表示名の対応表。
        Map<String, String> departmentDisplayMap = Map.of(
                "amami", "天美",
                "main", "本社"
        );

        // ⑩ 画面へ表示データを渡す。
        model.addAttribute("users", users);
        model.addAttribute("dates", dates);
        model.addAttribute("shiftMap", shiftMap);
        model.addAttribute(
                "staffingBalanceMap",
                staffingBalanceMap
        );
        model.addAttribute("department", department);

        // 年月は用途別に分けて渡し、テンプレート内の衝突を避ける。
        model.addAttribute("monthValue", monthValue);
        model.addAttribute("monthLabel", monthLabel);

        model.addAttribute(
                "departments",
                departmentDisplayMap.keySet()
        );
        model.addAttribute(
                "departmentNames",
                departmentDisplayMap
        );
        model.addAttribute(
                "selectedDepartmentName",
                departmentDisplayMap.getOrDefault(
                        department,
                        department
                )
        );

        return "shift/list";
    }
}