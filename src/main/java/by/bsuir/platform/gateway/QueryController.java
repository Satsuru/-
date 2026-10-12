package by.bsuir.platform.gateway;

import by.bsuir.platform.api.Contracts.BookingView;
import by.bsuir.platform.api.Contracts.HotelView;
import by.bsuir.platform.api.Contracts.NotificationView;
import by.bsuir.platform.api.Contracts.OccupancyRevenueView;
import by.bsuir.platform.api.Contracts.PeriodInput;
import by.bsuir.platform.api.Contracts.QuoteInput;
import by.bsuir.platform.api.Contracts.QuoteView;
import by.bsuir.platform.api.Contracts.RoomOfferView;
import by.bsuir.platform.api.Contracts.SearchInput;
import by.bsuir.platform.api.Contracts.UserView;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class QueryController {

    private final ReadGateway gateway;

    public QueryController(ReadGateway gateway) {
        this.gateway = gateway;
    }

    @QueryMapping
    public UserView me() {
        return gateway.me();
    }

    @QueryMapping
    public HotelView hotel(@Argument long id) {
        return gateway.hotel(id);
    }

    @QueryMapping
    public List<RoomOfferView> searchHotels(@Argument SearchInput input) {
        return gateway.searchHotels(input);
    }

    @QueryMapping
    public QuoteView quoteStay(@Argument QuoteInput input) {
        return gateway.quoteStay(input);
    }

    @QueryMapping
    public List<BookingView> myBookings() {
        return gateway.myBookings();
    }

    @QueryMapping
    public BookingView booking(@Argument long id) {
        return gateway.booking(id);
    }

    @QueryMapping
    public List<NotificationView> myNotifications() {
        return gateway.myNotifications();
    }

    @QueryMapping
    public OccupancyRevenueView occupancyAndRevenue(@Argument PeriodInput input) {
        return gateway.occupancyAndRevenue(input);
    }
}
