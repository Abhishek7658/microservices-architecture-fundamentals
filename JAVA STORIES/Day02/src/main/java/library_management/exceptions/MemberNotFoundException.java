package library_management.exceptions;

/** Thrown when a member id does not exist. */
public class MemberNotFoundException extends LibraryException {
    public MemberNotFoundException(String message) { super(message); }
}
