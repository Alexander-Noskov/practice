package academy.tochkavhoda.competition.dto.request;

import java.util.Set;

public record RegisterExpertRequest(
        String firstName,
        String lastName,
        String login,
        String password,
        Set<String> directions
) {
}
