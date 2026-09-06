package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.YearMonth;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
