package academy.tochkavhoda.competition.dto.response;

import academy.tochkavhoda.competition.model.Application;

import java.util.List;

public record GrantResult(
        List<Application> funded,
        List<Application> skippedByFund,
        List<Application> skippedByThreshold,
        long remainingFund
) {
}
