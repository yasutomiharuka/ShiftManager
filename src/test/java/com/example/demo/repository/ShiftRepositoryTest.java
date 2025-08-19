package com.example.demo.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.demo.model.Shift;
import com.example.demo.model.UserProfile;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ShiftRepositoryTest {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private UserProfileRepository userRepository;

    @Test
    public void testSaveAndFindShift() {
        // ユーザー作成・保存
        UserProfile user = new UserProfile();
        user.setUsername("shiftuser");
        user.setPassword("pass");
        user.setFirstName("Hanako");
        user.setLastName("Yamada");
        user.setDepartment("main");
        user.setEmploymentType("正社員");
        userRepository.save(user);

        // シフト作成・保存
        Shift shift = new Shift();
        shift.setUser(user);
        shift.setDate(LocalDate.of(2025, 7, 20));
        shift.setShiftType("日");
        shift.setTimeSlot("9:00-14:00");
        shift.setDepartment("main");
        shift.setTemporary(false);
        shift.setFixed(false);
        shiftRepository.save(shift);

        // 検証
        List<Shift> result = shiftRepository.findAll();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getUser().getUsername()).isEqualTo("shiftuser");
        assertThat(result.get(0).getShiftType()).isEqualTo("日");
    }
}
