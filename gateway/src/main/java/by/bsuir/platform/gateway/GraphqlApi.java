package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Api;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class GraphqlApi {

    private final PlatformApi api;

    public GraphqlApi(PlatformApi api) {
        this.api = api;
    }

    @QueryMapping
    public Api.UserView me() {
        return api.me();
    }

    @QueryMapping
    public Api.HotelView hotel(@Argument long id) {
        return api.hotel(id);
    }

    @QueryMapping
    public List<Api.RoomOfferView> searchHotels(@Argument Api.SearchInput input) {
        return api.searchHotels(input);
    }

    @QueryMapping
    public Api.QuoteView quoteStay(@Argument Api.QuoteInput input) {
        return api.quoteStay(input);
    }

    @QueryMapping
    public List<Api.BookingView> myBookings(@Argument Api.BookingFilter filter) {
        return api.myBookings(filter);
    }

    @QueryMapping
    public Api.BookingView booking(@Argument long id) {
        return api.booking(id);
    }

    @QueryMapping
    public Api.BookingStats myBookingStats() {
        return api.myBookingStats();
    }

    @QueryMapping
    public List<Api.NotificationView> myNotifications() {
        return api.myNotifications();
    }

    @QueryMapping
    public Api.OccupancyRevenueView occupancyAndRevenue(@Argument Api.PeriodInput input) {
        return api.occupancyAndRevenue(input);
    }

    @MutationMapping
    public Api.AuthPayload register(@Argument Api.RegisterInput input) {
        return api.register(input);
    }

    @MutationMapping
    public Api.AuthPayload login(@Argument Api.LoginInput input) {
        return api.login(input);
    }

    @MutationMapping
    public boolean logout() {
        return api.logout();
    }

    @MutationMapping
    public Api.BookingView createBooking(@Argument Api.CreateBookingInput input) {
        return api.createBooking(input);
    }

    @MutationMapping
    public Api.BookingView payBooking(@Argument Api.PayInput input) {
        return api.payBooking(input);
    }

    @MutationMapping
    public Api.BookingView cancelBooking(@Argument Api.CancelInput input) {
        return api.cancelBooking(input);
    }

    @MutationMapping
    public Api.DialogView askAssistant(@Argument Api.AskInput input) {
        return api.askAssistant(input);
    }
}
