package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Shift;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.UserProfileRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShiftDraftSaveIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    private UserProfile nurse;

    @BeforeEach
    void setUp() {
        shiftRepository.deleteAll();
        userProfileRepository.deleteAll();

        UserProfile profile = new UserProfile();
        profile.setUsername("nurse1");
        profile.setPassword("password");
        profile.setRole("USER");
        profile.setFirstName("花子");
        profile.setLastName("看護師");
        profile.setBirthDate(LocalDate.of(1990, 1, 1));
        profile.setGender("female");
        profile.setEmploymentType("正社員");
        profile.setDepartment("amami");

        nurse = userProfileRepository.save(profile);
    }

    @Test
    void draftShiftAppearsOnGenerateScreen() throws Exception {
        String dateKey = "2024-05-10";
        String cellKey = nurse.getId() + "_" + dateKey;

        mockMvc.perform(post("/api/shift/request/save")
                .with(user("user").roles("USER"))
                .with(csrf())
                .param("action", "DRAFT")
                .param("department", "amami")
                .param("targetMonth", "2024-05")
                .param("shifts[" + cellKey + "]", "夜"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/shift/generate**"));

        List<Shift> stored = shiftRepository.findAll();
        assertThat(stored).hasSize(1);
        Shift saved = stored.get(0);
        assertThat(saved.getShiftType()).isEqualTo("夜");
        assertThat(saved.getStatus()).isEqualTo(Shift.Status.DRAFT);

        MvcResult result = mockMvc.perform(get("/shift/generate")
                .with(user("user").roles("USER"))
                .param("department", "amami")
                .param("month", "2024-05"))
            .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> model = result.getModelAndView().getModel();
        @SuppressWarnings("unchecked")
        Map<String, String> shiftMap = (Map<String, String>) model.get("shiftMap");

        assertThat(shiftMap).isNotNull();
        assertThat(shiftMap).containsEntry(cellKey, "夜");
    }
}
