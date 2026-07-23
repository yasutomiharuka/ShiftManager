package com.example.demo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Shift;
import com.example.demo.model.ShiftRequirement;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.ShiftRequirementRepository;

/** 日付・時間帯別の人員過不足を算出するサービス。 */
@Service
public class StaffingBalanceService {

    public static final List<String> TIME_SLOTS =
            List.of("9-14", "14-16", "16-18", "NIGHT");

    private final ShiftRepository shiftRepository;
    private final ShiftRequirementRepository shiftRequirementRepository;

    public StaffingBalanceService(
            ShiftRepository shiftRepository,
            ShiftRequirementRepository shiftRequirementRepository) {
        this.shiftRepository = shiftRepository;
        this.shiftRequirementRepository = shiftRequirementRepository;
    }

    /**
     * キーを「yyyy-MM-dd_時間帯」、値を「実際の勤務人数－必要人数」とするMapを返す。
     * 必要人員が未登録のキーはMapへ追加しない。
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> buildStaffingBalanceMap(String department, YearMonth targetMonth) {
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        List<Shift> shifts =
                shiftRepository.findByDepartmentAndDateBetween(department, startDate, endDate);
        List<ShiftRequirement> requirements =
                shiftRequirementRepository.findByDepartmentAndDateBetween(
                        department, startDate, endDate);

        Map<String, Integer> actualCountMap = new HashMap<>();
        for (Shift shift : shifts) {
            if (shift == null || shift.getDate() == null) {
                continue;
            }
            for (String timeSlot : TIME_SLOTS) {
                if (isCoveredTimeSlot(shift, timeSlot)) {
                    actualCountMap.merge(toKey(shift.getDate(), timeSlot), 1, Integer::sum);
                }
            }
        }

        Map<String, Integer> balanceMap = new HashMap<>();
        for (ShiftRequirement requirement : requirements) {
            if (requirement == null
                    || requirement.getDate() == null
                    || requirement.getTimeSlot() == null
                    || requirement.getRequiredCount() == null) {
                continue;
            }

            String timeSlot = normalizeTimeSlot(requirement.getTimeSlot());
            if (!TIME_SLOTS.contains(timeSlot)) {
                continue;
            }

            String key = toKey(requirement.getDate(), timeSlot);
            int actualCount = actualCountMap.getOrDefault(key, 0);
            balanceMap.put(key, calculateBalance(actualCount, requirement.getRequiredCount()));
        }
        return balanceMap;
    }

    public int calculateBalance(int actualCount, int requiredCount) {
        return actualCount - requiredCount;
    }

    boolean isCoveredTimeSlot(Shift shift, String targetTimeSlot) {
        String shiftType = shift.getShiftType() == null ? "" : shift.getShiftType().trim();
        return switch (shiftType) {
            case "日" -> !"NIGHT".equals(targetTimeSlot);
            case "9-14" -> "9-14".equals(targetTimeSlot);
            case "14-16" -> "14-16".equals(targetTimeSlot);
            case "16-18" -> "16-18".equals(targetTimeSlot);
            case "14-18" -> "14-16".equals(targetTimeSlot) || "16-18".equals(targetTimeSlot);
            case "夜" -> "NIGHT".equals(targetTimeSlot);
            case "臨(確)", "臨(自)" ->
                    normalizeTimeSlot(shift.getTimeSlot()).equals(targetTimeSlot);
            default -> false;
        };
    }

    private String normalizeTimeSlot(String timeSlot) {
        if (timeSlot == null) {
            return "";
        }
        return switch (timeSlot.trim().toUpperCase()) {
            case "09-14", "09:00-14:00", "9:00-14:00" -> "9-14";
            case "14:00-16:00" -> "14-16";
            case "16:00-18:00" -> "16-18";
            case "14:00-18:00" -> "14-18";
            case "夜間", "夜間帯" -> "NIGHT";
            default -> timeSlot.trim().toUpperCase();
        };
    }

    private String toKey(LocalDate date, String timeSlot) {
        return date + "_" + timeSlot;
    }
}
