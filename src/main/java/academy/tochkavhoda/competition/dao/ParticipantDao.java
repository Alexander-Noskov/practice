package academy.tochkavhoda.competition.dao;

import academy.tochkavhoda.competition.model.Participant;

public interface ParticipantDao {
    Participant save(Participant participant);
    Participant findByLogin(String login);
    Participant findByCompanyName(String companyName);
    void delete(String login);
    boolean existsByCompanyName(String companyName);
}
