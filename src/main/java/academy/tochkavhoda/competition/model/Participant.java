package academy.tochkavhoda.competition.model;

public class Participant extends User {
    private final String companyName;

    public Participant(String login, String passwordHash, String firstName, String lastName, String companyName) {
        super(login, passwordHash, firstName, lastName);
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }
}
