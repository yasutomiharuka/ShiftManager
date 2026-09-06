package com.example.demo.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.form.ShiftRequirementForm;
import com.example.demo.service.ShiftGenerationService;
import com.example.demo.service.ShiftRequirementService;
import com.example.demo.service.ShiftService;
import com.example.demo.service.UserProfileService;

class ShiftGenerationControllerTest {

    private ShiftGenerationService shiftGenerationService;
    private UserProfileService userProfileService;
    private ShiftService shiftService;
    private ShiftRequirementService shiftRequirementService;
    private ShiftGenerationController controller;

    @BeforeEach
    void setUp() {
        shiftGenerationService = mock(ShiftGenerationService.class);
        userProfileService = mock(UserProfileService.class);
        shiftService = mock(ShiftService.class);
        shiftRequirementService = mock(ShiftRequirementService.class);
        controller = new ShiftGenerationController(
                shiftGenerationService,
                userProfileService,
                shiftService,
                shiftRequirementService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 過去月に保存済みシフトがある無効職員を表示対象にする() {
        YearMonth pastMonth = YearMonth.now(ZoneId.of("Asia/Tokyo")).minusMonths(1);
        UserProfileDto active = user(1L, "amami");
        UserProfileDto inactive = user(2L, "main");
        String inactiveShiftKey = "2_" + pastMonth.atDay(1);

        when(userProfileService.getAllUserProfiles()).thenReturn(List.of(active));
        when(userProfileService.getInactiveUserProfiles()).thenReturn(List.of(inactive));
        when(shiftService.getShiftMap(anyList(), eq(pastMonth.atDay(1)
                .datesUntil(pastMonth.atEndOfMonth().plusDays(1)).toList()), eq("amami")))
                .thenReturn(Map.of(inactiveShiftKey, "日"));
        when(shiftRequirementService.buildForm("amami", pastMonth))
                .thenReturn(new ShiftRequirementForm());

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.showGeneratePage("amami", pastMonth, model);

        @SuppressWarnings("unchecked")
        List<UserProfileDto> displayedUsers =
                (List<UserProfileDto>) model.get("users");

        @SuppressWarnings("unchecked")
        Set<Long> inactiveUserIds =
                (Set<Long>) model.get("inactiveUserIds");

        assertThat(view).isEqualTo("shift/generate");
        assertThat(displayedUsers).containsExactly(active, inactive);
        assertThat(inactiveUserIds).containsExactly(2L);
        assertThat(model.get("pastMonth")).isEqualTo(true);
    }

    @Test
    void 過去月のPostはServiceを呼ばず生成画面へ戻す() {
        YearMonth pastMonth = YearMonth.now(ZoneId.of("Asia/Tokyo")).minusMonths(1);
        ShiftGenerationForm form = new ShiftGenerationForm();
        form.setDepartment("amami");
        form.setTargetMonth(pastMonth);
        form.setShifts(Map.of());
        BindingResult bindingResult = mock(BindingResult.class);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        when(bindingResult.hasErrors()).thenReturn(false);

        String view = controller.generateShift(
                form,
                bindingResult,
                "amami",
                pastMonth.toString(),
                redirect);

        assertThat(view).isEqualTo("redirect:/shift/generate");
        assertThat(redirect.getFlashAttributes().get("errorMessage"))
                .isEqualTo("過去月のシフトは自動生成できません。必要な修正は手動で行ってください。");
        verify(shiftGenerationService, never())
                .generateComplementShift("amami", pastMonth.toString());
    }

    private static UserProfileDto user(Long id, String department) {
        UserProfileDto user = new UserProfileDto();
        user.setId(id);
        user.setDepartment(department);
        user.setFirstName("姓" + id);
        user.setLastName("名" + id);
        return user;
    }
}
