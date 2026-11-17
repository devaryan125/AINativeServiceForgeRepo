package com.serviceforge.controller;

import com.serviceforge.service.TechnicianAvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TechnicianAvailabilityService availabilityService;

    @Test
    void returnsConflictResponseWhenTechnicianBookingOverlaps() throws Exception {
        doThrow(new IllegalStateException("Technician Jordan Reyes already has a job overlapping "
                + "2026-11-12T10:00 to 2026-11-12T12:00"))
                .when(availabilityService)
                .bookJob(eq(1L), eq("Overlapping Customer"), any(), any());

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 1,
                                  "customerName": "Overlapping Customer",
                                  "startTime": "2026-11-12T10:00:00",
                                  "endTime": "2026-11-12T12:00:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("overlapping")));
    }

    @Test
    void returnsBadRequestWhenEndTimeIsNotAfterStartTime() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 1,
                                  "customerName": "Invalid Customer",
                                  "startTime": "2026-11-12T10:00:00",
                                  "endTime": "2026-11-12T10:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End time must be after start time"));

        verifyNoInteractions(availabilityService);
    }

    @Test
    void returnsBadRequestWhenStartOrEndTimeIsMissing() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 1,
                                  "customerName": "Missing Start Customer",
                                  "endTime": "2026-11-12T11:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("must not be null"));

        verifyNoInteractions(availabilityService);
    }
}
