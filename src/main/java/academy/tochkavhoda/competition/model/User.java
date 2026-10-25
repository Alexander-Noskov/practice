package academy.tochkavhoda.competition.model;

import java.util.Objects;

public class User {
    private final String login;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;

    public User(String login, String passwordHash, String firstName, String lastName) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(login, user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(login);
    }
}