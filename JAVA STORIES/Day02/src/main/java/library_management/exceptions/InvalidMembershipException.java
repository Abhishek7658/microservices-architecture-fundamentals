package library_management.exceptions;

/** Thrown when a member's membership is expired or invalid. */
public class InvalidMembershipException extends LibraryException {
    public InvalidMembershipException(String message) { super(message); }
}
