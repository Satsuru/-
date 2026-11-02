package by.bsuir.platform.gateway;

import by.bsuir.platform.identity.IdentityApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
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

    private static ConfigurableApplicationContext auth;

    static {
        SpringApplication application = new SpringApplication(IdentityApplication.class);
        auth = application.run("--server.port=0");
        int port = ((WebServerApplicationContext) auth).getWebServer().getPort();
        System.setProperty("auth.service.url", "http://127.0.0.1:" + port);
    }

    @AfterAll
    static void stopAuth() {
        if (auth != null) {
            auth.close();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    @Qualifier("bookingJdbc")
    JdbcTemplate bookingJdbc;

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
        String guest = token("guest@hotel.local", "guest");
        mvc.perform(as("""
                {
                  me { email roles }
                  myBookings(filter: {status: "CREATED"}) { id status totalPrice snapshot { occupancyMultiplier demandMultiplier calculatedPrice } }
                  booking(id: 2) { status totalPrice }
                  myBookingStats { total created paid paidSum }
                  myNotifications { kind }
                }
                """, guest))
                .andExpect(jsonPath("$.data.me.email").value("guest@hotel.local"))
                .andExpect(jsonPath("$.data.me.roles[0]").value("GUEST"))
                .andExpect(jsonPath("$.data.myBookings[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data.myBookings[0].totalPrice").value(312.0))
                .andExpect(jsonPath("$.data.myBookings[0].snapshot.occupancyMultiplier").value(1.3))
                .andExpect(jsonPath("$.data.myBookings[0].snapshot.demandMultiplier").value(1.0))
                .andExpect(jsonPath("$.data.myBookings[0].snapshot.calculatedPrice").value(312.0))
                .andExpect(jsonPath("$.data.booking.totalPrice").value(closeTo(308.88, 0.001)))
                .andExpect(jsonPath("$.data.myBookingStats.created").value(1))
                .andExpect(jsonPath("$.data.myBookingStats.paid").value(1))
                .andExpect(jsonPath("$.data.myNotifications.length()").value(2));

        mvc.perform(as("""
                { occupancyAndRevenue(input: {hotelId: 1, from: "2026-06-01", to: "2026-06-30"}) { occupancy revenue } }
                """, guest))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("администратору")));

        String admin = token("admin@hotel.local", "admin");
        mvc.perform(as("""
                { occupancyAndRevenue(input: {hotelId: 1, from: "2026-06-01", to: "2026-06-30"}) { occupancy revenue } }
                """, admin))
                .andExpect(jsonPath("$.data.occupancyAndRevenue.occupancy").value(0.5))
                .andExpect(jsonPath("$.data.occupancyAndRevenue.revenue").value(closeTo(308.88, 0.001)));
    }

    @Test
    void filterSortAndMutations() throws Exception {
        String guest = token("guest@hotel.local", "guest");
        mvc.perform(as("""
                { myBookings(filter: {status: "PAID", sort: DATE_DESC}) { id status } }
                """, guest))
                .andExpect(jsonPath("$.data.myBookings.length()").value(1))
                .andExpect(jsonPath("$.data.myBookings[0].status").value("PAID"));

        String created = mvc.perform(as("""
                mutation {
                  createBooking(input: {roomId: 1, checkIn: "2026-07-06", checkOut: "2026-07-08", guests: 2}) {
                    id status totalPrice
                  }
                }
                """, guest))
                .andExpect(jsonPath("$.data.createBooking.status").value("CREATED"))
                .andExpect(jsonPath("$.data.createBooking.totalPrice").value(200.0))
                .andReturn().getResponse().getContentAsString();
        int id = Integer.parseInt(JsonPath.read(created, "$.data.createBooking.id"));

        mvc.perform(as("""
                mutation { payBooking(input: {bookingId: %d, method: "CARD"}) { status } }
                """.formatted(id), guest))
                .andExpect(jsonPath("$.data.payBooking.status").value("PAID"));

        String admin = token("admin@hotel.local", "admin");
        mvc.perform(as("""
                { occupancyAndRevenue(input: {hotelId: 1, from: "2026-07-06", to: "2026-07-07"}) { occupancy revenue } }
                """, admin))
                .andExpect(jsonPath("$.data.occupancyAndRevenue.occupancy").value(0.5))
                .andExpect(jsonPath("$.data.occupancyAndRevenue.revenue").value(closeTo(200.0, 0.001)));

        mvc.perform(as("""
                mutation { cancelBooking(input: {bookingId: 1}) { status } }
                """, guest))
                .andExpect(jsonPath("$.data.cancelBooking.status").value("CANCELLED"));

        mvc.perform(as("""
                mutation { cancelBooking(input: {bookingId: 2}) { status } }
                """, guest))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("Оплаченную бронь")));

        mvc.perform(as("""
                mutation { askAssistant(input: {text: "Позовите сотрудника"}) { status } }
                """, guest))
                .andExpect(jsonPath("$.data.askAssistant.status").value("ESCALATED"));
    }

    @Test
    void expiredHoldCancelsBookingAndFreesNights() throws Exception {
        String guest = token("guest@hotel.local", "guest");
        bookingJdbc.update(
                "update booking.bookings set hold_until = ? where id = 1",
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(5))
        );
        mvc.perform(as("""
                mutation { payBooking(input: {bookingId: 1, method: "CARD"}) { status } }
                """, guest))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("Срок оплаты истёк")));
        mvc.perform(gql("""
                { quoteStay(input: {roomId: 1, checkIn: "2026-06-10", checkOut: "2026-06-12", guests: 2}) { total } }
                """))
                .andExpect(jsonPath("$.data.quoteStay.total").isNumber());
        mvc.perform(as("{ booking(id: 1) { status } }", guest))
                .andExpect(jsonPath("$.data.booking.status").value("CANCELLED"));
    }

    @Test
    void failedHoldRollsBookingBack() throws Exception {
        String guest = token("guest@hotel.local", "guest");
        mvc.perform(as("""
                mutation {
                  createBooking(input: {roomId: 1, checkIn: "2026-07-06", checkOut: "2026-07-08", guests: 2}) { id status }
                }
                """, guest))
                .andExpect(jsonPath("$.data.createBooking.status").value("CREATED"));
        mvc.perform(as("""
                mutation {
                  createBooking(input: {roomId: 1, checkIn: "2026-07-06", checkOut: "2026-07-08", guests: 2}) { id }
                }
                """, guest))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("занят")));
        mvc.perform(as("{ booking(id: 4) { status } }", guest))
                .andExpect(jsonPath("$.data.booking.status").value("CANCELLED"));
        mvc.perform(as("""
                { myBookings(filter: {status: "CREATED"}) { id } }
                """, guest))
                .andExpect(jsonPath("$.data.myBookings.length()").value(2));
    }

    @Test
    void logoutDropsSessionAndWorkerCanCancelUnpaid() throws Exception {
        String guest = token("guest@hotel.local", "guest");
        mvc.perform(as("mutation { logout }", guest))
                .andExpect(jsonPath("$.data.logout").value(true));
        mvc.perform(as("{ me { email } }", guest))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("Authorization")));

        String worker = token("worker@hotel.local", "worker");
        mvc.perform(as("""
                mutation { cancelBooking(input: {bookingId: 1}) { status } }
                """, worker))
                .andExpect(jsonPath("$.data.cancelBooking.status").value("CANCELLED"));
    }

    @Test
    void missingUserAndBusyRoomAreErrors() throws Exception {
        mvc.perform(gql("{ me { email } }"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("Authorization")));
        mvc.perform(gql("""
                { quoteStay(input: {roomId: 2, checkIn: "2026-06-01", checkOut: "2026-06-03", guests: 2}) { total } }
                """))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("занят")));
    }

    private String token(String email, String password) throws Exception {
        String body = mvc.perform(gql("""
                mutation { login(input: {email: "%s", password: "%s"}) { token } }
                """.formatted(email, password))).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.login.token");
    }

    private MockHttpServletRequestBuilder as(String query, String token) throws Exception {
        return gql(query).header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder gql(String query) throws Exception {
        return post("/graphql").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("query", query)));
    }
}
