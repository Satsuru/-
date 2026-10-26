package by.bsuir.platform.contracts;

public final class BookingRules {

    private BookingRules() {
    }

    public static void checkPay(String status) {
        if (!"CREATED".equals(status)) {
            throw new Failures.BadRequestException("Оплатить можно только бронь в статусе CREATED");
        }
    }

    public static void checkCancel(String status) {
        if ("PAID".equals(status)) {
            throw new Failures.BadRequestException("Оплаченную бронь отменить нельзя");
        }
        if (!"CREATED".equals(status)) {
            throw new Failures.BadRequestException("Отменить можно только неоплаченную бронь");
        }
    }
}
