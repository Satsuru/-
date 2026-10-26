package by.bsuir.platform.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GatewayApiTest {

    @Autowired
    MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void searchAndQuoteKeepJunePrice() throws Exception {
        mvc.perform(gql("""
                {
                  searchHotels(input: {city: "Минск", checkIn: "2026-06-01", checkOut: "2026-06-03", guests: 2, sort: PRICE_ASC}) {
                    roomType nightlyPrice hotel { name }
                  }
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.searchHotels[0].hotel.name").value("Неман"))
                .andExpect(jsonPath("$.data.searchHotels[0].nightlyPrice").value(132.0))
                .andExpect(jsonPath("$.data.searchHotels.length()").value(1));

        mvc.perform(gql("""
                {
                  quoteStay(input: {roomId: 1, checkIn: "2026-06-01", checkOut: "2026-06-03", guests: 2}) {
                    total multipliers { season demand }
                  }
                }
                """))
                .andExpect(jsonPath("$.data.quoteStay.total").value(264.0))
                .andExpect(jsonPath("$.data.quoteStay.multipliers.season").value(1.2))
                .andExpect(jsonPath("$.data.quoteStay.multipliers.demand").value(1.1));
    }

    @Test
    void seedBookingMatchesHoldAndMultipliers() throws Exception {
        mvc.perform(gql("""
                {
                  me { email roles }
                  myBookings(filter: {status: "CREATED"}) { id status totalPrice snapshot { occupancyMultiplier demandMultiplier calculatedPrice } }
                  booking(id: 2) { status totalPrice }
                  myBookingStats { total created paid paidSum }
                  myNotifications { kind }
                  occupancyAndRevenue(input: {hotelId: 1, from: "2026-06-01", to: "2026-06-30"}) { occupancy revenue }
                }
                """).header("X-User-Id", "1"))
                .andExpect(jsonPath("$.data.me.email").value("guest@hotel.local"))
                .andExpect(jsonPath("$.data.myBookings[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data.myBookings[0].totalPrice").value(312.0))
                .andExpect(jsonPath("$.data.myBookings[0].snapshot.occupancyMultiplier").value(1.3))
                .andExpect(jsonPath("$.data.myBookings[0].snapshot.demandMultiplier").value(1.0))
                .andExpect(jsonPath("$.data.myBookings[0].snapshot.calculatedPrice").value(312.0))
                .andExpect(jsonPath("$.data.booking.totalPrice").value(closeTo(308.88, 0.001)))
                .andExpect(jsonPath("$.data.myBookingStats.created").value(1))
                .andExpect(jsonPath("$.data.myBookingStats.paid").value(1))
                .andExpect(jsonPath("$.data.myNotifications.length()").value(2))
                .andExpect(jsonPath("$.data.occupancyAndRevenue.occupancy").value(0.5))
                .andExpect(jsonPath("$.data.occupancyAndRevenue.revenue").value(closeTo(308.88, 0.001)));
    }

    @Test
    void filterSortAndMutations() throws Exception {
        mvc.perform(gql("""
                { myBookings(filter: {status: "PAID", sort: DATE_DESC}) { id status } }
                """).header("X-User-Id", "1"))
                .andExpect(jsonPath("$.data.myBookings.length()").value(1))
                .andExpect(jsonPath("$.data.myBookings[0].status").value("PAID"));

        mvc.perform(gql("""
                mutation {
                  createBooking(input: {roomId: 1, checkIn: "2026-07-06", checkOut: "2026-07-08", guests: 2}) {
                    status totalPrice
                  }
                }
                """).header("X-User-Id", "1"))
                .andExpect(jsonPath("$.data.createBooking.status").value("CREATED"))
                .andExpect(jsonPath("$.data.createBooking.totalPrice").value(200.0));

        mvc.perform(gql("""
                mutation { payBooking(input: {bookingId: 101, method: "CARD"}) { status } }
                """).header("X-User-Id", "1"))
                .andExpect(jsonPath("$.data.payBooking.status").value("PAID"));

        mvc.perform(gql("""
                mutation { cancelBooking(input: {bookingId: 1}) { status } }
                """).header("X-User-Id", "1"))
                .andExpect(jsonPath("$.data.cancelBooking.status").value("CANCELLED"));

        mvc.perform(gql("""
                mutation { cancelBooking(input: {bookingId: 2}) { status } }
                """).header("X-User-Id", "1"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("Оплаченную бронь")));

        mvc.perform(gql("""
                mutation { login(input: {email: "guest@hotel.local", password: "guest"}) { token user { id } } }
                """))
                .andExpect(jsonPath("$.data.login.token").value("user-1"));

        mvc.perform(gql("""
                mutation { askAssistant(input: {text: "Позовите сотрудника"}) { status messages { author } } }
                """).header("X-User-Id", "1"))
                .andExpect(jsonPath("$.data.askAssistant.status").value("ESCALATED"));
    }

    @Test
    void missingUserAndBusyRoomAreErrors() throws Exception {
        mvc.perform(gql("{ me { email } }"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("X-User-Id")));
        mvc.perform(gql("""
                { quoteStay(input: {roomId: 2, checkIn: "2026-06-01", checkOut: "2026-06-03", guests: 2}) { total } }
                """))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("занят")));
    }

    private MockHttpServletRequestBuilder gql(String query) throws Exception {
        return post("/graphql").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("query", query)));
    }
}
