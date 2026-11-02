package by.bsuir.platform.contracts;

public final class Failures {

    private Failures() {
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    public static class HoldExpiredException extends BadRequestException {
        private final long bookingId;

        public HoldExpiredException(long bookingId) {
            super("Срок оплаты истёк");
            this.bookingId = bookingId;
        }

        public long bookingId() {
            return bookingId;
        }
    }
}
