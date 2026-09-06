package com.example.demo.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.model.Shift;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.service.ShiftService;

class ShiftEditControllerTest {

    private ShiftService shiftService;
    private UserProfileRepository userProfileRepository;
    private ShiftRepository shiftRepository;
    private ShiftEditController controller;

    @BeforeEach
    void setUp() {
        shiftService = mock(ShiftService.class);
        userProfileRepository = mock(UserProfileRepository.class);
        shiftRepository = mock(ShiftRepository.class);
        controller = new ShiftEditController(
                shiftService,
                userProfileRepository,
                shiftRepository);
    }

    @Test
    void 対象月に履歴がある無効職員は一時保存できる() {
        YearMonth month = YearMonth.of(2026, 8);
        UserProfile inactive = user(10L, false);
        Shift history = shift(inactive, month);
        ShiftGenerationForm form = form(month);
        BindingResult bindingResult = mock(BindingResult.class);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        when(bindingResult.hasErrors()).thenReturn(false);
        when(userProfileRepository.findByDepartmentAndActiveTrue("amami"))
                .thenReturn(List.of());
        when(shiftRepository.findByDepartmentAndDateBetween(
                "amami", month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(history));

        String view = controller.save(
                form, bindingResult, "DRAFT", "amami", redirect);

        assertThat(view).isEqualTo("redirect:/shift/generate");
        verify(shiftService).saveShifts(form, Shift.Status.DRAFT);
        assertThat(redirect.getFlashAttributes().get("notice"))
                .isEqualTo("シフトを一時保存しました。");
    }

    @Test
    void 履歴がない無効職員は保存処理へ進まない() {
        YearMonth month = YearMonth.of(2026, 8);
        ShiftGenerationForm form = form(month);
        BindingResult bindingResult = mock(BindingResult.class);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        when(bindingResult.hasErrors()).thenReturn(false);
        when(userProfileRepository.findByDepartmentAndActiveTrue("amami"))
                .thenReturn(List.of());
        when(shiftRepository.findByDepartmentAndDateBetween(
                "amami", month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of());

        String view = controller.save(
                form, bindingResult, "DRAFT", "amami", redirect);

        assertThat(view).isEqualTo("redirect:/shift/generate");
        verify(shiftService, never()).saveShifts(form, Shift.Status.DRAFT);
        assertThat(redirect.getFlashAttributes().get("errorMessage"))
                .asString()
                .contains("保存済みシフトがない無効職員");
    }

    private static ShiftGenerationForm form(YearMonth month) {
        ShiftGenerationForm form = new ShiftGenerationForm();
        form.setDepartment("amami");
        form.setTargetMonth(month);
        form.setShifts(Map.of("10_" + month.atDay(10), "休"));
        return form;
    }

    private static UserProfile user(Long id, boolean active) {
        UserProfile user = new UserProfile();
        user.setId(id);
        user.setDepartment("amami");
        user.setActive(active);
        return user;
    }

    private static Shift shift(UserProfile user, YearMonth month) {
        Shift shift = new Shift();
        shift.setUser(user);
        shift.setDepartment("amami");
        shift.setDate(month.atDay(10));
        shift.setShiftType("日");
        shift.setStatus(Shift.Status.CONFIRMED);
        return shift;
    }
}
