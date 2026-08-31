package com.krushna.moviebooking.booking.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.krushna.moviebooking.booking.exception.SeatNotFoundException;
import com.krushna.moviebooking.booking.exception.SeatUnavailableException;
import com.krushna.moviebooking.booking.exception.ShowNotFoundException;
import com.krushna.moviebooking.booking.exception.ShowServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Unit tests for {@link HttpShowClient} using {@link MockRestServiceServer}.
 * All tests exercise real HTTP error-mapping logic without a running server.
 */
class HttpShowClientTest {

    private MockRestServiceServer server;
    private HttpShowClient client;

    private static final UUID SHOW_ID  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SEAT_ID1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SEAT_ID2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        RestClient restClient = RestClient.create(restTemplate);
        client = new HttpShowClient(restClient);
    }

    // ------------------------------------------------------------------ //
    //  existsShow
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("existsShow returns true when show-service returns 200")
    void existsShow_Success() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess("{\"id\":\"" + SHOW_ID + "\"}", MediaType.APPLICATION_JSON));

        boolean result = client.existsShow(SHOW_ID);

        assertThat(result).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("existsShow returns false when show-service returns 404")
    void existsShow_NotFound_ReturnsFalse() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withStatus(HttpStatus.NOT_FOUND));

        boolean result = client.existsShow(SHOW_ID);

        assertThat(result).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("existsShow throws ShowServiceUnavailableException on 503")
    void existsShow_ServiceUnavailable_ThrowsException() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.existsShow(SHOW_ID))
                .isInstanceOf(ShowServiceUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("existsShow returns false for null showId without making any HTTP call")
    void existsShow_NullShowId_ReturnsFalseWithoutCallingServer() {
        boolean result = client.existsShow(null);
        assertThat(result).isFalse();
        server.verify();
    }

    // ------------------------------------------------------------------ //
    //  getShowById
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("getShowById returns populated Optional on success")
    void getShowById_Success() {
        String showJson = "{\"id\":\"" + SHOW_ID + "\",\"movieId\":\"" + UUID.randomUUID()
                + "\",\"theatreId\":\"" + UUID.randomUUID()
                + "\",\"screenId\":\"" + UUID.randomUUID()
                + "\",\"status\":\"SCHEDULED\""
                + ",\"startTime\":\"2030-01-01T10:00:00Z\""
                + ",\"endTime\":\"2030-01-01T13:00:00Z\"}";

        server.expect(requestTo("/api/v1/shows/" + SHOW_ID))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess(showJson, MediaType.APPLICATION_JSON));

        Optional<ShowClient.ShowDto> result = client.getShowById(SHOW_ID);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(SHOW_ID);
        assertThat(result.get().status()).isEqualTo("SCHEDULED");
        server.verify();
    }

    @Test
    @DisplayName("getShowById returns empty Optional when show-service returns 404")
    void getShowById_NotFound_ReturnsEmpty() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<ShowClient.ShowDto> result = client.getShowById(SHOW_ID);

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("getShowById throws ShowServiceUnavailableException on 500")
    void getShowById_ServerError_ThrowsException() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getShowById(SHOW_ID))
                .isInstanceOf(ShowServiceUnavailableException.class);
        server.verify();
    }

    // ------------------------------------------------------------------ //
    //  getShowSeatsByIds
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("getShowSeatsByIds returns matching seats from show-service response")
    void getShowSeatsByIds_Success() {
        UUID physSeatId1 = UUID.randomUUID();
        UUID physSeatId2 = UUID.randomUUID();
        String seatsJson = "["
                + "{\"id\":\"" + SEAT_ID1 + "\",\"showId\":\"" + SHOW_ID + "\",\"seatId\":\"" + physSeatId1
                + "\",\"seatNumber\":\"A1\",\"price\":250.00,\"status\":\"AVAILABLE\"},"
                + "{\"id\":\"" + SEAT_ID2 + "\",\"showId\":\"" + SHOW_ID + "\",\"seatId\":\"" + physSeatId2
                + "\",\"seatNumber\":\"A2\",\"price\":250.00,\"status\":\"AVAILABLE\"}"
                + "]";

        server.expect(requestTo("/api/v1/shows/" + SHOW_ID + "/seats"))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess(seatsJson, MediaType.APPLICATION_JSON));

        List<ShowClient.ShowSeatDto> result = client.getShowSeatsByIds(SHOW_ID, List.of(SEAT_ID1, SEAT_ID2));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ShowClient.ShowSeatDto::id)
                          .containsExactlyInAnyOrder(SEAT_ID1, SEAT_ID2);
        assertThat(result).allMatch(s -> "AVAILABLE".equals(s.status()));
        server.verify();
    }

    @Test
    @DisplayName("getShowSeatsByIds throws ShowNotFoundException when show-service returns 404")
    void getShowSeatsByIds_ShowNotFound_ThrowsShowNotFoundException() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID + "/seats"))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getShowSeatsByIds(SHOW_ID, List.of(SEAT_ID1)))
                .isInstanceOf(ShowNotFoundException.class);
        server.verify();
    }

    @Test
    @DisplayName("getShowSeatsByIds returns empty list when seatIds is empty without any HTTP call")
    void getShowSeatsByIds_EmptySeatIds_ReturnsEmptyWithoutCallingServer() {
        List<ShowClient.ShowSeatDto> result = client.getShowSeatsByIds(SHOW_ID, List.of());
        assertThat(result).isEmpty();
        server.verify();
    }

    // ------------------------------------------------------------------ //
    //  updateShowSeatsStatus
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("updateShowSeatsStatus succeeds silently on 200 from show-service")
    void updateShowSeatsStatus_Success_Booked() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID + "/seats/status"))
              .andExpect(method(HttpMethod.PUT))
              .andRespond(withSuccess());

        client.updateShowSeatsStatus(SHOW_ID, List.of(SEAT_ID1, SEAT_ID2), "BOOKED");

        server.verify();
    }

    @Test
    @DisplayName("updateShowSeatsStatus throws SeatUnavailableException on 409 Conflict")
    void updateShowSeatsStatus_Conflict_ThrowsSeatUnavailableException() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID + "/seats/status"))
              .andExpect(method(HttpMethod.PUT))
              .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.updateShowSeatsStatus(SHOW_ID, List.of(SEAT_ID1), "BOOKED"))
                .isInstanceOf(SeatUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("updateShowSeatsStatus throws SeatNotFoundException on 404 Not Found")
    void updateShowSeatsStatus_NotFound_ThrowsSeatNotFoundException() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID + "/seats/status"))
              .andExpect(method(HttpMethod.PUT))
              .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.updateShowSeatsStatus(SHOW_ID, List.of(SEAT_ID1), "BOOKED"))
                .isInstanceOf(SeatNotFoundException.class);
        server.verify();
    }

    @Test
    @DisplayName("updateShowSeatsStatus throws ShowServiceUnavailableException on 503")
    void updateShowSeatsStatus_503_ThrowsShowServiceUnavailableException() {
        server.expect(requestTo("/api/v1/shows/" + SHOW_ID + "/seats/status"))
              .andExpect(method(HttpMethod.PUT))
              .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.updateShowSeatsStatus(SHOW_ID, List.of(SEAT_ID1), "BOOKED"))
                .isInstanceOf(ShowServiceUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("updateShowSeatsStatus is a no-op when seatIds list is empty")
    void updateShowSeatsStatus_EmptySeatIds_NoOp() {
        client.updateShowSeatsStatus(SHOW_ID, List.of(), "BOOKED");
        server.verify(); // no expectations — verify zero HTTP calls
    }
}
