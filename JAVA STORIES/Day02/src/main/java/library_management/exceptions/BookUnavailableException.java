package library_management.exceptions;

/** Thrown when no copies of a book are available. */
public class BookUnavailableException extends LibraryException {
    public BookUnavailableException(String message) { super(message); }
}
