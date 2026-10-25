package academy.tochkavhoda.competition.dto.request;

public record RegisterParticipantRequest(
        String firstName,
        String lastName,
        String login,
        String password,
        String companyName
) {
}
