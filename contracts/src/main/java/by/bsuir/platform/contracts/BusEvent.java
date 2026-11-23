package by.bsuir.platform.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Полезная нагрузка сообщения Kafka. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusEvent(
        String type,
        Long bookingId,
        Long userId,
        Long hotelId,
        String amount,
        String checkIn,
        String checkOut,
        Integer totalRooms,
        Long dialogId
) {
    public static final String BOOKING_CREATED = "BOOKING_CREATED";
    public static final String BOOKING_PAID = "BOOKING_PAID";
    public static final String BOOKING_CANCELLED = "BOOKING_CANCELLED";
    public static final String ASSISTANT_ESCALATED = "ASSISTANT_ESCALATED";

    private static final ObjectMapper JSON = new ObjectMapper();

    public String json() {
        try {
            return JSON.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static BusEvent parse(String raw) {
        try {
            return JSON.readValue(raw, BusEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(raw, ex);
        }
    }
}
