package by.bsuir.platform.gateway;

import by.bsuir.platform.contracts.Api;
import by.bsuir.platform.contracts.Clients;
import by.bsuir.platform.contracts.ProtoMaps;
import by.bsuir.platform.contracts.Rpc;
import by.bsuir.platform.contracts.grpc.AnalyticsApiGrpc;
import by.bsuir.platform.contracts.grpc.AssistantApiGrpc;
import by.bsuir.platform.contracts.grpc.AskRequest;
import by.bsuir.platform.contracts.grpc.BookingApiGrpc;
import by.bsuir.platform.contracts.grpc.CancelBookingRequest;
import by.bsuir.platform.contracts.grpc.CatalogApiGrpc;
import by.bsuir.platform.contracts.grpc.CreateBookingRequest;
import by.bsuir.platform.contracts.grpc.Empty;
import by.bsuir.platform.contracts.grpc.GetBookingRequest;
import by.bsuir.platform.contracts.grpc.HoldRequest;
import by.bsuir.platform.contracts.grpc.Id;
import by.bsuir.platform.contracts.grpc.ListBookingsRequest;
import by.bsuir.platform.contracts.grpc.NotificationApiGrpc;
import by.bsuir.platform.contracts.grpc.PayBookingRequest;
import by.bsuir.platform.contracts.grpc.PeriodRequest;
import by.bsuir.platform.contracts.grpc.PriceRequest;
import by.bsuir.platform.contracts.grpc.PricingApiGrpc;
import by.bsuir.platform.contracts.grpc.RoomStayRequest;
import by.bsuir.platform.contracts.grpc.SearchRequest;
import by.bsuir.platform.contracts.grpc.UserRequest;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Шлюз вызывает сервисы по сети. Внутри процесса их базы больше не поднимаются. */
@Configuration
public class GrpcServiceClients {

    @Bean(name = "catalogChannel", destroyMethod = "shutdown")
    ManagedChannel catalogChannel(@Value("${services.catalog.host:127.0.0.1}") String host,
                                  @Value("${services.catalog.port:9082}") int port) {
        return Rpc.channel(host, port);
    }

    @Bean(name = "pricingChannel", destroyMethod = "shutdown")
    ManagedChannel pricingChannel(@Value("${services.pricing.host:127.0.0.1}") String host,
                                  @Value("${services.pricing.port:9083}") int port) {
        return Rpc.channel(host, port);
    }

    @Bean(name = "bookingChannel", destroyMethod = "shutdown")
    ManagedChannel bookingChannel(@Value("${services.booking.host:127.0.0.1}") String host,
                                  @Value("${services.booking.port:9084}") int port) {
        return Rpc.channel(host, port);
    }

    @Bean(name = "notificationChannel", destroyMethod = "shutdown")
    ManagedChannel notificationChannel(@Value("${services.notification.host:127.0.0.1}") String host,
                                       @Value("${services.notification.port:9087}") int port) {
        return Rpc.channel(host, port);
    }

    @Bean(name = "assistantChannel", destroyMethod = "shutdown")
    ManagedChannel assistantChannel(@Value("${services.assistant.host:127.0.0.1}") String host,
                                    @Value("${services.assistant.port:9086}") int port) {
        return Rpc.channel(host, port);
    }

    @Bean(name = "analyticsChannel", destroyMethod = "shutdown")
    ManagedChannel analyticsChannel(@Value("${services.analytics.host:127.0.0.1}") String host,
                                    @Value("${services.analytics.port:9089}") int port) {
        return Rpc.channel(host, port);
    }

    @Bean
    public Clients.CatalogClient catalogClient(@Qualifier("catalogChannel") ManagedChannel catalogChannel) {
        CatalogApiGrpc.CatalogApiBlockingStub stub = CatalogApiGrpc.newBlockingStub(catalogChannel);
        return new Clients.CatalogClient() {
            @Override
            public Optional<Api.HotelView> hotel(long hotelId) {
                return Rpc.call(() -> ProtoMaps.hotel(Rpc.deadline(stub).hotel(Id.newBuilder().setId(hotelId).build())));
            }

            @Override
            public List<Api.RoomCard> search(String city, LocalDate checkIn, LocalDate checkOut, int guests, Double minStars) {
                SearchRequest.Builder request = SearchRequest.newBuilder()
                        .setCity(city)
                        .setCheckIn(checkIn.toString())
                        .setCheckOut(checkOut.toString())
                        .setGuests(guests);
                if (minStars != null) {
                    request.setHasMinStars(true).setMinStars(minStars);
                }
                return Rpc.call(() -> ProtoMaps.rooms(Rpc.deadline(stub).search(request.build())));
            }

            @Override
            public Optional<Api.RoomCard> getRoom(long roomId, LocalDate checkIn, LocalDate checkOut) {
                return Rpc.call(() -> ProtoMaps.room(Rpc.deadline(stub).getRoom(stay(roomId, checkIn, checkOut))));
            }

            @Override
            public boolean isFree(long roomId, LocalDate checkIn, LocalDate checkOut) {
                return Rpc.call(() -> Rpc.deadline(stub).isFree(stay(roomId, checkIn, checkOut)).getValue());
            }

            @Override
            public int activeRooms(long hotelId) {
                return Rpc.call(() -> Rpc.deadline(stub).activeRooms(Id.newBuilder().setId(hotelId).build()).getValue());
            }

            @Override
            public void hold(long roomId, LocalDate checkIn, LocalDate checkOut, long bookingId) {
                Rpc.call(() -> Rpc.deadline(stub).hold(HoldRequest.newBuilder()
                        .setRoomId(roomId)
                        .setCheckIn(checkIn.toString())
                        .setCheckOut(checkOut.toString())
                        .setBookingId(bookingId)
                        .build()));
            }

            @Override
            public void release(long bookingId) {
                Rpc.call(() -> Rpc.deadline(stub).release(Id.newBuilder().setId(bookingId).build()));
            }

            @Override
            public void confirmSold(long bookingId) {
                Rpc.call(() -> Rpc.deadline(stub).confirmSold(Id.newBuilder().setId(bookingId).build()));
            }
        };
    }

    @Bean
    public Clients.PricingClient pricingClient(@Qualifier("pricingChannel") ManagedChannel pricingChannel) {
        PricingApiGrpc.PricingApiBlockingStub stub = PricingApiGrpc.newBlockingStub(pricingChannel);
        return (hotelId, basePrice, occupancyRatio, checkIn, checkOut, guests) -> Rpc.call(() -> ProtoMaps.quote(
                Rpc.deadline(stub).calculate(PriceRequest.newBuilder()
                        .setHotelId(hotelId)
                        .setBasePrice(basePrice.toPlainString())
                        .setOccupancyRatio(occupancyRatio.toPlainString())
                        .setCheckIn(checkIn.toString())
                        .setCheckOut(checkOut.toString())
                        .setGuests(guests)
                        .build())));
    }

    @Bean
    public Clients.BookingClient bookingClient(@Qualifier("bookingChannel") ManagedChannel bookingChannel) {
        BookingApiGrpc.BookingApiBlockingStub stub = BookingApiGrpc.newBlockingStub(bookingChannel);
        return new Clients.BookingClient() {
            @Override
            public List<Api.BookingView> listMine(long userId, String status, Api.BookingSort sort) {
                return Rpc.call(() -> ProtoMaps.bookings(Rpc.deadline(stub).listMine(ListBookingsRequest.newBuilder()
                        .setUserId(userId)
                        .setStatus(status == null ? "" : status)
                        .setSort(sort == null ? "" : sort.name())
                        .build())));
            }

            @Override
            public Optional<Api.BookingView> get(long bookingId, long userId) {
                return Rpc.call(() -> ProtoMaps.booking(Rpc.deadline(stub).get(GetBookingRequest.newBuilder()
                        .setBookingId(bookingId)
                        .setUserId(userId)
                        .build())));
            }

            @Override
            public Api.BookingStats stats(long userId) {
                return Rpc.call(() -> ProtoMaps.stats(Rpc.deadline(stub).stats(UserRequest.newBuilder().setUserId(userId).build())));
            }

            @Override
            public Api.BookingView create(long userId, long roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
                return Rpc.call(() -> ProtoMaps.booking(Rpc.deadline(stub).create(CreateBookingRequest.newBuilder()
                        .setUserId(userId)
                        .setRoomId(roomId)
                        .setCheckIn(checkIn.toString())
                        .setCheckOut(checkOut.toString())
                        .setGuests(guests)
                        .build())).orElseThrow());
            }

            @Override
            public Api.BookingView pay(long userId, long bookingId, String method) {
                return Rpc.call(() -> ProtoMaps.booking(Rpc.deadline(stub).pay(PayBookingRequest.newBuilder()
                        .setUserId(userId)
                        .setBookingId(bookingId)
                        .setMethod(method)
                        .build())).orElseThrow());
            }

            @Override
            public Api.BookingView cancel(long actorId, boolean staff, long bookingId) {
                return Rpc.call(() -> ProtoMaps.booking(Rpc.deadline(stub).cancel(CancelBookingRequest.newBuilder()
                        .setActorId(actorId)
                        .setStaff(staff)
                        .setBookingId(bookingId)
                        .build())).orElseThrow());
            }

            @Override
            public void sweep() {
                Rpc.call(() -> Rpc.deadline(stub).sweep(Empty.getDefaultInstance()));
            }
        };
    }

    @Bean
    public Clients.NotificationClient notificationClient(@Qualifier("notificationChannel") ManagedChannel notificationChannel) {
        NotificationApiGrpc.NotificationApiBlockingStub stub = NotificationApiGrpc.newBlockingStub(notificationChannel);
        return userId -> Rpc.call(() -> ProtoMaps.notifications(Rpc.deadline(stub).listMine(UserRequest.newBuilder().setUserId(userId).build())));
    }

    @Bean
    public Clients.AssistantClient assistantClient(@Qualifier("assistantChannel") ManagedChannel assistantChannel) {
        AssistantApiGrpc.AssistantApiBlockingStub stub = AssistantApiGrpc.newBlockingStub(assistantChannel);
        return (userId, dialogId, guestText, answer, escalated) -> Rpc.call(() -> ProtoMaps.dialog(Rpc.deadline(stub).ask(AskRequest.newBuilder()
                .setUserId(userId)
                .setDialogId(dialogId == null ? 0 : dialogId)
                .setHasDialog(dialogId != null)
                .setGuestText(guestText)
                .setAnswer(answer)
                .setEscalated(escalated)
                .build())));
    }

    @Bean
    public Clients.AnalyticsClient analyticsClient(@Qualifier("analyticsChannel") ManagedChannel analyticsChannel) {
        AnalyticsApiGrpc.AnalyticsApiBlockingStub stub = AnalyticsApiGrpc.newBlockingStub(analyticsChannel);
        return (hotelId, from, to) -> Rpc.call(() -> ProtoMaps.occupancy(Rpc.deadline(stub).occupancyAndRevenue(PeriodRequest.newBuilder()
                .setHotelId(hotelId)
                .setFrom(from.toString())
                .setTo(to.toString())
                .build())));
    }

    private static RoomStayRequest stay(long roomId, LocalDate checkIn, LocalDate checkOut) {
        return RoomStayRequest.newBuilder()
                .setRoomId(roomId)
                .setCheckIn(checkIn.toString())
                .setCheckOut(checkOut.toString())
                .build();
    }
}
