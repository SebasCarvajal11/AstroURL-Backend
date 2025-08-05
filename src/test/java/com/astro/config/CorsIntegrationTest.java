package com.astro.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CorsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenPreflightRequestFromAllowedOrigin_thenReturnsOkWithCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/url")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type, Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")));
    }

    @Test
    void whenActualRequestFromAllowedOrigin_thenReturnsSuccessWithCorsHeader() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Origin", "http://localhost:3000")
                        .contentType("application/json")
                        .content("{\"username\":\"cors-user\",\"email\":\"cors@test.com\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void whenRequestFromDisallowedOrigin_thenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Origin", "http://disallowed-domain.com")
                        .contentType("application/json")
                        .content("{\"username\":\"cors-user2\",\"email\":\"cors2@test.com\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }
}