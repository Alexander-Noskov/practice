package academy.tochkavhoda.competition.dto.request;

import java.util.Set;

public record CreateApplicationRequest(
        String title,
        String description,
        Set<String> directions,
        Long requestedAmount
) {
}
