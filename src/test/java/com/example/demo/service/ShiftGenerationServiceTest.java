package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.demo.model.Shift;
import com.example.demo.model.ShiftRequirement;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.ShiftRequirementRepository;
import com.example.demo.repository.TemporaryWorkerAssignmentRepository;
import com.example.demo.repository.UserProfileRepository;

class ShiftGenerationServiceTest {

    private UserProfileRepository userProfileRepository;
    private ShiftRequirementRepository shiftRequirementRepository;
    private TemporaryWorkerAssignmentRepository temporaryWorkerAssignmentRepository;
    private ShiftRepository shiftRepository;
    private ShiftGenerationService service;

    @BeforeEach
    void setUp() {
        userProfileRepository = mock(UserProfileRepository.class);
        shiftRequirementRepository = mock(ShiftRequirementRepository.class);
        temporaryWorkerAssignmentRepository = mock(TemporaryWorkerAssignmentRepository.class);
        shiftRepository = mock(ShiftRepository.class);
        service = new ShiftGenerationService(
                userProfileRepository,
                shiftRequirementRepository,
                temporaryWorkerAssignmentRepository,
                shiftRepository);
    }

    @Test
    void 過去月はRepositoryへアクセスせず自動生成を拒否する() {
        YearMonth pastMonth = YearMonth.now(ZoneId.of("Asia/Tokyo")).minusMonths(1);

        assertThatThrownBy(() ->
                service.generateComplementShift("amami", pastMonth.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Past month shifts cannot be generated");

        verifyNoInteractions(
                userProfileRepository,
                shiftRequirementRepository,
                temporaryWorkerAssignmentRepository,
                shiftRepository);
    }

    @Test
    void 必要人数を満たした後も勤務可能なパート全員へ割り当てる() {
        YearMonth targetMonth = YearMonth.now(ZoneId.of("Asia/Tokyo")).plusMonths(1);
        LocalDate targetDate = targetMonth.atDay(1);
        UserProfile user1 = partTimeUser(1L);
        UserProfile user2 = partTimeUser(2L);
        UserProfile user3 = partTimeUser(3L);

        when(userProfileRepository.findByDepartmentAndActiveTrue("amami"))
                .thenReturn(List.of(user1, user2, user3));
        when(shiftRepository.findByDepartmentAndDateBetween(
                "amami", targetMonth.atDay(1), targetMonth.atEndOfMonth()))
                .thenReturn(List.of());
        when(shiftRequirementRepository.findByDepartmentAndDateBetween(
                "amami", targetMonth.atDay(1), targetMonth.atEndOfMonth()))
                .thenReturn(List.of(new ShiftRequirement(targetDate, "amami", "9-14", 1)));

        service.generateComplementShift("amami", targetMonth.toString());

        ArgumentCaptor<Shift> captor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepository, atLeastOnce()).save(captor.capture());

        List<Shift> shiftsOnTargetDate = captor.getAllValues().stream()
                .filter(shift -> targetDate.equals(shift.getDate()))
                .toList();

        assertThat(shiftsOnTargetDate).hasSize(3);
        assertThat(shiftsOnTargetDate)
                .extracting(shift -> shift.getUser().getId())
                .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(shiftsOnTargetDate)
                .allMatch(shift -> "9-14".equals(shift.getShiftType()));
    }

    private UserProfile partTimeUser(Long id) {
        UserProfile user = new UserProfile();
        user.setId(id);
        user.setFirstName("姓" + id);
        user.setLastName("名" + id);
        user.setEmploymentType("パート");
        setWorkingTime(user, DayOfWeek.MONDAY);
        setWorkingTime(user, DayOfWeek.TUESDAY);
        setWorkingTime(user, DayOfWeek.WEDNESDAY);
        setWorkingTime(user, DayOfWeek.THURSDAY);
        setWorkingTime(user, DayOfWeek.FRIDAY);
        setWorkingTime(user, DayOfWeek.SATURDAY);
        setWorkingTime(user, DayOfWeek.SUNDAY);
        return user;
    }

    private void setWorkingTime(UserProfile user, DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY -> {
                user.setMondayStartTime("09:00");
                user.setMondayEndTime("14:00");
            }
            case TUESDAY -> {
                user.setTuesdayStartTime("09:00");
                user.setTuesdayEndTime("14:00");
            }
            case WEDNESDAY -> {
                user.setWednesdayStartTime("09:00");
                user.setWednesdayEndTime("14:00");
            }
            case THURSDAY -> {
                user.setThursdayStartTime("09:00");
                user.setThursdayEndTime("14:00");
            }
            case FRIDAY -> {
                user.setFridayStartTime("09:00");
                user.setFridayEndTime("14:00");
            }
            case SATURDAY -> {
                user.setSaturdayStartTime("09:00");
                user.setSaturdayEndTime("14:00");
            }
            case SUNDAY -> {
                user.setSundayStartTime("09:00");
                user.setSundayEndTime("14:00");
            }
        }
    }
}
