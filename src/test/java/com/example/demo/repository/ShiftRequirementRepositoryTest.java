package com.example.demo.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.demo.model.ShiftRequirement;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // PostgreSQL使用時
public class ShiftRequirementRepositoryTest {

    @Autowired
    private ShiftRequirementRepository shiftRequirementRepository;

    @Test
    public void testSaveAndFindByDateAndDepartment() {
        LocalDate date = LocalDate.of(2025, 7, 21);

        ShiftRequirement req = new ShiftRequirement();
        req.setDate(date);
        req.setDepartment("main");
        req.setTimeSlot("9:00-14:00");
        req.setRequiredCount(3);
        shiftRequirementRepository.save(req);

        List<ShiftRequirement> result = shiftRequirementRepository.findByDateAndDepartment(date, "main");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTimeSlot()).isEqualTo("9:00-14:00");
    }

    @Test
    public void testFindByDepartmentAndDateBetween() {
        LocalDate date1 = LocalDate.of(2025, 7, 21);
        LocalDate date2 = LocalDate.of(2025, 7, 22);

        shiftRequirementRepository.save(createReq("main", date1, "9:00-14:00", 2));
        shiftRequirementRepository.save(createReq("main", date2, "14:00-16:00", 1));

        List<ShiftRequirement> results = shiftRequirementRepository.findByDepartmentAndDateBetween("main", date1, date2);
        assertThat(results).hasSize(2);
    }

    @Test
    public void testFindByDateAndDepartmentAndTimeSlot() {
        LocalDate date = LocalDate.of(2025, 7, 23);

        shiftRequirementRepository.save(createReq("main", date, "16:00-18:00", 2));

        List<ShiftRequirement> result = shiftRequirementRepository.findByDateAndDepartmentAndTimeSlot(date, "main", "16:00-18:00");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRequiredCount()).isEqualTo(2);
    }

    private ShiftRequirement createReq(String department, LocalDate date, String slot, int count) {
        ShiftRequirement req = new ShiftRequirement();
        req.setDate(date);
        req.setDepartment(department);
        req.setTimeSlot(slot);
        req.setRequiredCount(count);
        return req;
    }
}
