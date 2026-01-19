package org.example.cloudopsadmin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    public void testGetMonthlyAnalysis() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/monthly"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetYearlyAnalysis() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/yearly"))
                .andExpect(status().isOk());
    }
}
