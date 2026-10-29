package library_management.service;

import library_management.exceptions.ValidationException;

/** Small input validation helpers. */
final class Validators {
    private Validators() {}

    static String text(String field, String value) {
        if (value == null || value.isBlank()) throw new ValidationException(field + " must not be empty");
        return value.trim();
    }

    static int positive(String field, int value) {
        if (value <= 0) throw new ValidationException(field + " must be greater than 0");
        return value;
    }

    static String email(String value) {
        String v = text("Email", value);
        if (!v.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) throw new ValidationException("Invalid email: " + v);
        return v;
    }
}
