package library_management.service;

import library_management.exceptions.InvalidMembershipException;
import library_management.exceptions.MemberNotFoundException;
import library_management.model.Member;
import library_management.repository.Repository;

import java.time.LocalDate;
import java.util.List;

/** Register, update, delete members and validate membership. */
public class MemberService {
    private final Repository<Member> members;

    public MemberService(Repository<Member> members) { this.members = members; }

    public Member register(String name, String email, int membershipMonths) {
        Member m = new Member(Validators.text("Name", name), Validators.email(email),
                LocalDate.now().plusMonths(Validators.positive("Months", membershipMonths)));
        members.save(m);
        return m;
    }

    public Member get(String id) {
        return members.findById(id).orElseThrow(() -> new MemberNotFoundException("No member with id " + id));
    }

    public Member update(String id, String name, String email) {
        Member m = get(id);
        m.setName(Validators.text("Name", name));
        m.setEmail(Validators.email(email));
        members.save(m);
        return m;
    }

    public void delete(String id) {
        get(id);
        members.deleteById(id);
    }

    /** @throws InvalidMembershipException if expired */
    public Member validate(String id, LocalDate today) {
        Member m = get(id);
        if (!m.isValidOn(today))
            throw new InvalidMembershipException("Membership of " + m.getName() + " expired on " + m.getMembershipExpiry());
        return m;
    }

    public List<Member> all() { return members.findAll(); }
}
