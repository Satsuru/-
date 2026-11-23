package by.bsuir.platform.contracts;

/** Имена топиков Kafka. Синхронные ответы через них не передаются. */
public final class Topics {

    public static final String BOOKING_CREATED = "booking.created";
    public static final String BOOKING_PAID = "booking.paid";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String ASSISTANT_ESCALATED = "assistant.escalated";

    private Topics() {
    }
}
