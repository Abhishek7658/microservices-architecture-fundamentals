package library_management.exceptions;

/** Thrown when a book id does not exist. */
public class BookNotFoundException extends LibraryException {
    public BookNotFoundException(String message) { super(message); }
}
