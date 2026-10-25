package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.dao.TokenDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.AuthException;
import academy.tochkavhoda.competition.exception.NotFoundException;

public class InMemoryTokenDao implements TokenDao {

    private final Database db;

    public InMemoryTokenDao() {
        this.db = Database.getInstance();
    }

    @Override
    public void save(String token, String login) {
        db.getTokens().put(token, login);
        db.getTokenByLogin().put(login, token);
    }

    @Override
    public String findLoginByToken(String token) {
        String login = db.getTokens().get(token);
        if (login == null) {
            throw new AuthException("invalid token");
        }
        return login;
    }

    @Override
    public void delete(String token) {
        String login = db.getTokens().remove(token);
        if (login != null) {
            db.getTokenByLogin().remove(login);
        }
    }

    @Override
    public void deleteByLogin(String login) {
        String token = db.getTokenByLogin().remove(login);
        if (token != null) {
            db.getTokens().remove(token);
        }
    }
}
