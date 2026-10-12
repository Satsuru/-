package by.bsuir.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReadQueryTest {

    @Autowired
    MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void searchAndQuoteUseCatalogAndPricing() throws Exception {
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body("""
                {
                  searchHotels(input: {city: "Минск", checkIn: "2026-06-01", checkOut: "2026-06-03", guests: 2}) {
                    roomId
                    nightlyPrice
                    hotel { name }
                  }
                }
                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.searchHotels[0].hotel.name").value("Неман"))
                .andExpect(jsonPath("$.data.searchHotels[0].nightlyPrice").value(132.0))
                .andExpect(jsonPath("$.data.searchHotels.length()").value(1));

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body("""
                {
                  quoteStay(input: {roomId: 1, checkIn: "2026-06-01", checkOut: "2026-06-03", guests: 2}) {
                    total
                    multipliers { season demand }
                  }
                }
                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quoteStay.total").value(264.0))
                .andExpect(jsonPath("$.data.quoteStay.multipliers.season").value(1.2))
                .andExpect(jsonPath("$.data.quoteStay.multipliers.demand").value(1.1));
    }

    @Test
    void guestReadsOwnBookingsAndNotifications() throws Exception {
        mvc.perform(post("/graphql")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {
                                  me { email roles }
                                  myBookings { id status snapshot { calculatedPrice } }
                                  myNotifications { kind }
                                  booking(id: 2) { status }
                                }
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.me.email").value("guest@hotel.local"))
                .andExpect(jsonPath("$.data.me.roles[0]").value("GUEST"))
                .andExpect(jsonPath("$.data.myBookings.length()").value(2))
                .andExpect(jsonPath("$.data.myBookings[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data.myBookings[1].status").value("PAID"))
                .andExpect(jsonPath("$.data.booking.status").value("PAID"))
                .andExpect(jsonPath("$.data.myNotifications.length()").value(2));
    }

    @Test
    void analyticsReadsOnlyItsFacts() throws Exception {
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body("""
                {
                  occupancyAndRevenue(input: {hotelId: 1, from: "2026-05-01", to: "2026-06-30"}) {
                    occupancy
                    revenue
                  }
                }
                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occupancyAndRevenue.revenue").value(300.0))
                .andExpect(jsonPath("$.data.occupancyAndRevenue.occupancy").value(0.5));
    }

    @Test
    void missingUserAndBusyRoomAreErrors() throws Exception {
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body("{ me { email } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].message").value(containsString("X-User-Id")));

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body("""
                {
                  quoteStay(input: {roomId: 2, checkIn: "2026-06-01", checkOut: "2026-06-03", guests: 2}) {
                    total
                  }
                }
                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].message").value(containsString("занят")));
    }

    private String body(String query) throws Exception {
        return json.writeValueAsString(Map.of("query", query));
    }
}
