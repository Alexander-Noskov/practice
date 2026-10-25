package academy.tochkavhoda.competition.dao;

import academy.tochkavhoda.competition.model.User;

public interface UserDao {
    User save(User user);
    User findByLogin(String login);
    void delete(String login);
    boolean existsByLogin(String login);
}
