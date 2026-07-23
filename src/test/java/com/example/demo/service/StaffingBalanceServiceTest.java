package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.model.Shift;
import com.example.demo.model.ShiftRequirement;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.ShiftRequirementRepository;

@ExtendWith(MockitoExtension.class)
class StaffingBalanceServiceTest {

    @Mock ShiftRepository shiftRepository;
    @Mock ShiftRequirementRepository shiftRequirementRepository;

    @Test
    void 日勤を日中3時間帯へ加算して過不足を計算する() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        when(shiftRepository.findByDepartmentAndDateBetween("amami", date, LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(shift(date, "日")));
        when(shiftRequirementRepository.findByDepartmentAndDateBetween("amami", date, LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(requirement(date, "9-14", 2),
                        requirement(date, "14-16", 1), requirement(date, "16-18", 0)));

        Map<String, Integer> result = service().buildStaffingBalanceMap("amami", YearMonth.of(2026, 7));

        assertThat(result).containsEntry("2026-07-01_9-14", -1)
                .containsEntry("2026-07-01_14-16", 0)
                .containsEntry("2026-07-01_16-18", 1)
                .doesNotContainKey("2026-07-01_NIGHT");
    }

    @Test
    void 夜勤と時間帯勤務を該当時間帯だけへ加算する() {
        LocalDate date = LocalDate.of(2026, 7, 31);
        when(shiftRepository.findByDepartmentAndDateBetween("amami", LocalDate.of(2026, 7, 1), date))
                .thenReturn(List.of(shift(date, "夜"), shift(date, "14-18")));
        when(shiftRequirementRepository.findByDepartmentAndDateBetween("amami", LocalDate.of(2026, 7, 1), date))
                .thenReturn(List.of(requirement(date, "9-14", 0),
                        requirement(date, "14-16", 1), requirement(date, "16-18", 2),
                        requirement(date, "NIGHT", 1)));

        Map<String, Integer> result = service().buildStaffingBalanceMap("amami", YearMonth.of(2026, 7));

        assertThat(result).containsEntry("2026-07-31_9-14", 0)
                .containsEntry("2026-07-31_14-16", 0)
                .containsEntry("2026-07-31_16-18", -1)
                .containsEntry("2026-07-31_NIGHT", 0);
    }

    private StaffingBalanceService service() {
        return new StaffingBalanceService(shiftRepository, shiftRequirementRepository);
    }

    private Shift shift(LocalDate date, String shiftType) {
        Shift shift = new Shift();
        shift.setDate(date);
        shift.setShiftType(shiftType);
        return shift;
    }

    private ShiftRequirement requirement(LocalDate date, String timeSlot, int requiredCount) {
        return new ShiftRequirement(date, "amami", timeSlot, requiredCount);
    }
}
