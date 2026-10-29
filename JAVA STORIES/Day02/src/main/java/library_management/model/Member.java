package library_management.model;

import java.time.LocalDate;

/** A library member with an expiry date for their membership. */
public class Member extends Entity {
    private String name;
    private String email;
    private LocalDate membershipExpiry;

    public Member(String name, String email, LocalDate membershipExpiry) {
        super("M");
        this.name = name;
        this.email = email;
        this.membershipExpiry = membershipExpiry;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public LocalDate getMembershipExpiry() { return membershipExpiry; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setMembershipExpiry(LocalDate d) { this.membershipExpiry = d; }

    /** @return true if the membership is still valid on the given date */
    public boolean isValidOn(LocalDate date) { return !membershipExpiry.isBefore(date); }

    @Override
    public String toString() {
        return String.format("%s | %s <%s> | valid till %s", id, name, email, membershipExpiry);
    }
}
