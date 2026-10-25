package academy.tochkavhoda.competition.dao;

public interface TokenDao {
    void save(String token, String login);
    String findLoginByToken(String token);
    void delete(String token);
    void deleteByLogin(String login);
}
