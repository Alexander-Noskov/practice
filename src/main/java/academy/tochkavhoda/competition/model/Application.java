package academy.tochkavhoda.competition.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Application {
    private final UUID id;

    private final String ownerLogin;

    private final String title;

    private final String description;

    private final Set<String> directions;

    private final Long requestedAmount;

    public Application(UUID id, String ownerLogin, String title, String description,
                       Set<String> directions, Long requestedAmount) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerLogin = requireNonBlank(ownerLogin, "ownerLogin");
        this.title = requireNonBlank(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.directions = normalizeDirections(directions);
        if (requestedAmount == null || requestedAmount < 0) {
            throw new IllegalArgumentException("requestedAmount must be >= 0");
        }
        this.requestedAmount = requestedAmount;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerLogin() {
        return ownerLogin;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getDirections() {
        return directions;
    }

    public Long getRequestedAmount() {
        return requestedAmount;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
