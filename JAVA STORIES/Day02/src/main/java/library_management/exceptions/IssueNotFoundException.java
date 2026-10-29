package library_management.exceptions;

/** Thrown when an issue record does not exist or is already returned. */
public class IssueNotFoundException extends LibraryException {
    public IssueNotFoundException(String message) { super(message); }
}
