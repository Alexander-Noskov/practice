package academy.tochkavhoda.competition.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class Expert extends User {
    private final Set<String> directions;

    public Expert(String login, String passwordHash, String firstName, String lastName, Set<String> directions) {
        super(login, passwordHash, firstName, lastName);
        this.directions = normalizeDirections(directions);
    }

    public Set<String> getDirections() {
        return directions;
    }

    private static Set<String> normalizeDirections(Set<String> src) {
        Objects.requireNonNull(src, "directions");
        if (src.isEmpty()) {
            throw new IllegalArgumentException("directions must not be empty");
        }
        Set<String> result = new LinkedHashSet<>();
        for (String d : src) {
            if (d == null || d.isBlank()) {
                throw new IllegalArgumentException("direction must not be blank");
            }
            result.add(d.trim().toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(result);
    }
}
