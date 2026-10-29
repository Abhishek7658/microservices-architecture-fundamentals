package library_management.exceptions;

/** Thrown when user input is blank or invalid. */
public class ValidationException extends LibraryException {
    public ValidationException(String message) { super(message); }
}
