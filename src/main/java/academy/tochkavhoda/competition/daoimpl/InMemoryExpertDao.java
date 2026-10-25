package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.dao.ExpertDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Expert;

public class InMemoryExpertDao implements ExpertDao {

    private final Database db;

    public InMemoryExpertDao() {
        this.db = Database.getInstance();
    }

    @Override
    public Expert save(Expert expert) {
        String login = expert.getLogin();

        if (db.getExpertsByLogin().containsKey(login)) {
            throw new DuplicateLoginException(login);
        }

        db.getExpertsByLogin().put(login, expert);
        return expert;
    }

    @Override
    public Expert findByLogin(String login) {
        Expert expert = db.getExpertsByLogin().get(login);
        if (expert == null) {
            throw new NotFoundException(login);
        }
        return expert;
    }

    @Override
    public void delete(String login) {
        Expert expert = db.getExpertsByLogin().get(login);
        if (expert == null) {
            throw new NotFoundException(login);
        }
        db.getExpertsByLogin().remove(login);
    }
}
