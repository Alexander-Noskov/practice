package academy.tochkavhoda.competition.dto.request;

import java.util.UUID;

public record RateRequest(UUID applicationId, int value) {
}
