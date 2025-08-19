package com.example.demo.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.demo.model.ShiftRequest;
import com.example.demo.model.UserProfile;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ShiftRequestRepositoryTest {

    @Autowired
    private ShiftRequestRepository shiftRequestRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    public void testSaveAndFindShiftRequest() {
        // ユーザー作成・保存
        UserProfile user = new UserProfile();
        user.setUsername("requestuser");
        user.setPassword("pass");
        user.setFirstName("Taro");
        user.setLastName("Tanaka");
        user.setDepartment("amami");
        user.setEmploymentType("正社員");
        userProfileRepository.save(user);

        // シフトリクエスト作成・保存
        ShiftRequest request = new ShiftRequest();
        request.setUser(user);
        request.setDate(LocalDate.of(2025, 7, 25));
        request.setRequestType("休");
        request.setDepartment("amami");
        shiftRequestRepository.save(request);

        // 検証
        List<ShiftRequest> result = shiftRequestRepository.findAll();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getUser().getUsername()).isEqualTo("requestuser");
        assertThat(result.get(0).getRequestType()).isEqualTo("休");
    }
}
