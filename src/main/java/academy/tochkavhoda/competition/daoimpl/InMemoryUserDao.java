package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.dao.UserDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.User;

public class InMemoryUserDao implements UserDao {

    private final Database db;

    public InMemoryUserDao() {
        this.db = Database.getInstance();
    }

    @Override
    public User save(User user) {
        String login = user.getLogin();

        if (db.getUsersByLogin().containsKey(login)) {
            throw new DuplicateLoginException(login);
        }

        db.getUsersByLogin().put(login, user);
        return user;
    }

    @Override
    public User findByLogin(String login) {
        User user = db.getUsersByLogin().get(login);
        if (user == null) {
            throw new NotFoundException(login);
        }
        return user;
    }

    @Override
    public void delete(String login) {
        User user = db.getUsersByLogin().get(login);
        if (user == null) {
            throw new NotFoundException(login);
        }
        db.getUsersByLogin().remove(login);
    }
    @Override
    public boolean existsByLogin(String login) {
        return db.getUsersByLogin().containsKey(login);
    }
}
