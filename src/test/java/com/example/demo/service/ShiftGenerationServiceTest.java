// src/test/java/com/example/demo/service/ShiftGenerationServiceTest.java
package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.demo.model.Shift;
import com.example.demo.model.ShiftRequest;
import com.example.demo.model.ShiftRequirement;
import com.example.demo.model.TemporaryWorkerAssignment;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.ShiftRequestRepository;
import com.example.demo.repository.ShiftRequirementRepository;
import com.example.demo.repository.TemporaryWorkerAssignmentRepository;
import com.example.demo.repository.UserProfileRepository;

class ShiftGenerationServiceTest {

    // --- モック ---
    private UserProfileRepository userRepo = mock(UserProfileRepository.class);
    private ShiftRequirementRepository reqRepo = mock(ShiftRequirementRepository.class);
    private ShiftRequestRepository requestRepo = mock(ShiftRequestRepository.class);
    private TemporaryWorkerAssignmentRepository tempRepo = mock(TemporaryWorkerAssignmentRepository.class);
    private ShiftRepository shiftRepo = mock(ShiftRepository.class);

    // テスト対象
    private ShiftGenerationService service;

    @BeforeEach
    void setUp() {
        service = new ShiftGenerationService(userRepo, reqRepo, requestRepo, tempRepo, shiftRepo);
    }

    @Test
    void 事前臨時を優先し残りを正_パで埋める_希望休と固定休は外す() {
        // Arrange（準備）
        String dept = "amami";
        int year = 2025, month = 8;
        LocalDate d1 = LocalDate.of(year, month, 10);

        // ユーザー：正/パ 2名 + 臨時1名（臨時は事前割当で使われる）
        UserProfile staff1  = user(1L, "s1", dept); // 正社員
        staff1.setSundayOff(false);
        UserProfile staff2  = user(2L, "s2", dept); // パート
        staff2.setSundayOff(false);
        UserProfile tempUser = user(3L, "temp", dept); // 臨時

        when(userRepo.findByDepartment(dept)).thenReturn(List.of(staff1, staff2, tempUser));

        // 必要人員：d1 の "9:00-14:00" に 2名必要
        ShiftRequirement r1 = req(d1, dept, "9:00-14:00", 2);
        when(reqRepo.findByDepartmentAndDateBetween(eq(dept), any(), any()))
                .thenReturn(List.of(r1));

        // 希望休：staff2 が d1 は休み（除外されるべき）
        ShiftRequest reqOff = shiftReq(staff2, d1, dept, "休");
        when(requestRepo.findByDepartmentAndDateBetween(eq(dept), any(), any()))
                .thenReturn(List.of(reqOff));

        // 事前臨時：tempUser を d1 "9:00-14:00" に1名割当
        TemporaryWorkerAssignment t1 = tempAssign(tempUser, d1, dept, "9:00-14:00");
        when(tempRepo.findByDepartmentAndDateBetween(eq(dept), any(), any()))
                .thenReturn(List.of(t1));

        // saveAll の引数を捕捉
        ArgumentCaptor<List<Shift>> captor = ArgumentCaptor.forClass(List.class);
        when(shiftRepo.saveAll(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act（実行）
        service.generateShifts(year, month, dept);

        // Assert（検証）
        // 1日分のみ saveAll が呼ばれる想定
        verify(shiftRepo, atLeastOnce()).saveAll(anyList());
        List<Shift> saved = captor.getAllValues().stream().flatMap(List::stream).toList();

        // 合計2件（必要人員2名）保存される
        assertThat(saved).hasSize(2);

        // 事前臨時は「臨(確)」
        Shift fixedTemp = saved.stream().filter(s -> "臨(確)".equals(s.getShiftType())).findFirst().orElseThrow();
        assertThat(fixedTemp.getUser().getUsername()).isEqualTo("temp");
        assertThat(fixedTemp.getDepartment()).isEqualTo(dept);
        assertThat(fixedTemp.getTimeSlot()).isEqualTo("9:00-14:00");
        assertThat(fixedTemp.isTemporary()).isTrue();
        assertThat(fixedTemp.isFixed()).isTrue();

        // 残り1名は staff1（staff2 は希望休で除外される）
        Shift day = saved.stream().filter(s -> "日".equals(s.getShiftType())).findFirst().orElseThrow();
        assertThat(day.getUser().getUsername()).isEqualTo("s1");
        assertThat(day.getDepartment()).isEqualTo(dept);
        assertThat(day.getTimeSlot()).isEqualTo("9:00-14:00");
        assertThat(day.isTemporary()).isFalse();
        assertThat(day.isFixed()).isFalse();
    }

    // ====== ヘルパ ======
    private static UserProfile user(Long id, String name, String dept) {
        UserProfile u = new UserProfile();
        u.setId(id);              // ★ ここでIDを与える
        u.setUsername(name);
        u.setDepartment(dept);
        return u;
    }
    
    private static ShiftRequirement req(LocalDate date, String dept, String slot, int count) {
        ShiftRequirement r = new ShiftRequirement();
        r.setDate(date);
        r.setDepartment(dept);
        r.setTimeSlot(slot);
        r.setRequiredCount(count);
        return r;
    }

    private static ShiftRequest shiftReq(UserProfile u, LocalDate date, String dept, String type) {
        ShiftRequest r = new ShiftRequest();
        r.setUser(u);
        r.setDate(date);
        r.setDepartment(dept);
        r.setRequestType(type);
        return r;
    }

    private static TemporaryWorkerAssignment tempAssign(UserProfile u, LocalDate date, String dept, String slot) {
        TemporaryWorkerAssignment t = new TemporaryWorkerAssignment();
        t.setUser(u);
        t.setDate(date);
        t.setDepartment(dept);
        t.setTimeSlot(slot);
        t.setFixed(true);
        return t;
    }
}
