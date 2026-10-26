package by.bsuir.platform.contracts;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class Dates {

    private Dates() {
    }

    public static LocalDate parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new Failures.BadRequestException("Заполните поле " + field);
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            throw new Failures.BadRequestException("Поле " + field + " должно быть датой ГГГГ-ММ-ДД");
        }
    }

    public static void requireStay(LocalDate checkIn, LocalDate checkOut, int guests) {
        if (!checkOut.isAfter(checkIn)) {
            throw new Failures.BadRequestException("Дата выезда должна быть позже даты заезда");
        }
        if (guests < 1) {
            throw new Failures.BadRequestException("Гостей должно быть хотя бы один");
        }
    }
}
