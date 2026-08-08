package com.krushna.moviebooking.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.notification.dto.TicketDto;
import com.krushna.moviebooking.notification.dto.TicketRequest;
import com.krushna.moviebooking.notification.entity.TicketStatus;
import com.krushna.moviebooking.notification.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController).build();
    }

    @Test
    void testGenerateTicketEndpoint() throws Exception {
        UUID bookingId = UUID.randomUUID();
        TicketRequest request = TicketRequest.builder()
                .bookingId(bookingId)
                .bookingReference("REF-7788")
                .movieTitle("Gladiator 2")
                .theatreName("IMAX PVR")
                .screenName("Screen 1")
                .seatNumbers("E5, E6")
                .amount(new BigDecimal("600.00"))
                .build();

        TicketDto dto = TicketDto.builder()
                .id(UUID.randomUUID())
                .ticketNumber("TKT-7788")
                .bookingId(bookingId)
                .bookingReference("REF-7788")
                .movieTitle("Gladiator 2")
                .theatreName("IMAX PVR")
                .screenName("Screen 1")
                .seatNumbers("E5, E6")
                .amount(new BigDecimal("600.00"))
                .status(TicketStatus.GENERATED)
                .build();

        when(ticketService.generateTicket(any(TicketRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value("TKT-7788"))
                .andExpect(jsonPath("$.bookingReference").value("REF-7788"));
    }

    @Test
    void testGetByBookingIdEndpoint() throws Exception {
        UUID bookingId = UUID.randomUUID();
        TicketDto dto = TicketDto.builder()
                .id(UUID.randomUUID())
                .ticketNumber("TKT-1122")
                .bookingId(bookingId)
                .bookingReference("REF-1122")
                .movieTitle("Dune 2")
                .theatreName("Cinepolis")
                .screenName("Screen 3")
                .seatNumbers("B10")
                .amount(new BigDecimal("350.00"))
                .status(TicketStatus.GENERATED)
                .build();

        when(ticketService.getTicketByBookingId(bookingId)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/tickets/booking/{bookingId}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value("TKT-1122"))
                .andExpect(jsonPath("$.bookingReference").value("REF-1122"));
    }

    @Test
    void testDownloadPdfEndpoint() throws Exception {
        String ref = "REF-9900";
        byte[] pdfBytes = "%PDF-1.4 Mock Ticket Content".getBytes();

        when(ticketService.getTicketPdfByBookingReference(ref)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/tickets/reference/{bookingReference}/pdf", ref))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"Ticket_REF-9900.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }
}
