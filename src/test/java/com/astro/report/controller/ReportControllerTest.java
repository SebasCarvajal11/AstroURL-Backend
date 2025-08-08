package com.astro.report.controller;

import com.astro.config.BaseControllerTest;
import com.astro.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReportControllerTest extends BaseControllerTest {

    @Test
    void getPdfReport_shouldSucceed_forSirioUser() throws Exception {
        String token = getAccessToken("sirio-reporter", "sirio-rep@test.com", "password123", "Sirio");
        User user = userRepository.findByIdentifierWithPlan("sirio-reporter").orElseThrow();
        createTestUrl("https://active.com", "active", user);

        MvcResult result = mockMvc.perform(get("/api/reports/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void getPdfReport_shouldFailWithForbidden_forPolarisUser() throws Exception {
        String token = getAccessToken("polaris-reporter", "polaris-rep@test.com", "password123", "Polaris");

        mockMvc.perform(get("/api/reports/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}