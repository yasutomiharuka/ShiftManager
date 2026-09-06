package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.model.Shift;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.UserProfileRepository;

class ShiftServiceTest {

    private ShiftRepository shiftRepository;
    private UserProfileRepository userProfileRepository;
    private ShiftService service;

    @BeforeEach
    void setUp() {
        shiftRepository = mock(ShiftRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        service = new ShiftService(shiftRepository, userProfileRepository);
    }

    @Test
    void 対象月に履歴がある無効職員のシフトを手動変更できる() {
        YearMonth month = YearMonth.of(2026, 8);
        LocalDate date = month.atDay(10);
        UserProfile inactiveUser = user(10L, "amami", false);
        Shift existing = shift(inactiveUser, date, "amami", "日");
        existing.setSourceType(Shift.SourceType.AUTO);

        when(shiftRepository.findByDepartmentAndDateBetween(
                "amami", month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(existing));
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(inactiveUser));
        when(shiftRepository.findByUser_IdAndDateAndDepartment(10L, date, "amami"))
                .thenReturn(Optional.of(existing));

        service.saveShifts(form(month, Map.of("10_2026-08-10", "休")), Shift.Status.DRAFT);

        assertThat(existing.getShiftType()).isEqualTo("休");
        assertThat(existing.getStatus()).isEqualTo(Shift.Status.DRAFT);
        assertThat(existing.getSourceType()).isEqualTo(Shift.SourceType.MANUAL);
        verify(shiftRepository).save(existing);
    }

    @Test
    void 対象月に履歴がない無効職員は保存できない() {
        YearMonth month = YearMonth.of(2026, 8);
        UserProfile inactiveUser = user(10L, "amami", false);

        when(shiftRepository.findByDepartmentAndDateBetween(
                "amami", month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of());
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() ->
                service.saveShifts(form(month, Map.of("10_2026-08-10", "日")), Shift.Status.DRAFT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has no shift history");

        verify(shiftRepository, never()).save(any(Shift.class));
        verify(shiftRepository, never()).deleteByUser_IdAndDateAndDepartment(
                anyLong(), any(LocalDate.class), anyString());
    }

    @Test
    void 不正なセルが混在する場合は有効なセルも更新しない() {
        YearMonth month = YearMonth.of(2026, 8);
        UserProfile inactiveUser = user(10L, "amami", false);
        Shift existing = shift(inactiveUser, month.atDay(10), "amami", "日");
        LinkedHashMap<String, String> shifts = new LinkedHashMap<>();
        shifts.put("10_2026-08-10", "休");
        shifts.put("999_2026-08-11", "日");

        when(shiftRepository.findByDepartmentAndDateBetween(
                "amami", month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(existing));
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(inactiveUser));
        when(userProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.saveShifts(form(month, shifts), Shift.Status.CONFIRMED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(shiftRepository, never()).save(any(Shift.class));
        verify(shiftRepository, never()).deleteByUser_IdAndDateAndDepartment(
                anyLong(), any(LocalDate.class), anyString());
    }

    private static ShiftGenerationForm form(YearMonth month, Map<String, String> shifts) {
        ShiftGenerationForm form = new ShiftGenerationForm();
        form.setDepartment("amami");
        form.setTargetMonth(month);
        form.setShifts(shifts);
        return form;
    }

    private static UserProfile user(Long id, String department, boolean active) {
        UserProfile user = new UserProfile();
        user.setId(id);
        user.setDepartment(department);
        user.setActive(active);
        return user;
    }

    private static Shift shift(UserProfile user, LocalDate date, String department, String type) {
        Shift shift = new Shift();
        shift.setUser(user);
        shift.setDate(date);
        shift.setDepartment(department);
        shift.setShiftType(type);
        shift.setStatus(Shift.Status.CONFIRMED);
        return shift;
    }
}
