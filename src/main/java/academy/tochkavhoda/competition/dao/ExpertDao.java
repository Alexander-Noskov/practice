package academy.tochkavhoda.competition.dao;

import academy.tochkavhoda.competition.model.Expert;

public interface ExpertDao {
    Expert save(Expert expert);
    Expert findByLogin(String login);
    void delete(String login);
}
