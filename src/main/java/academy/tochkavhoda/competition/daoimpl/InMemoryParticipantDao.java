package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.dao.ParticipantDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.DuplicateCompanyException;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Participant;

public class InMemoryParticipantDao implements ParticipantDao {
    private final Database db;

    public InMemoryParticipantDao() {
        this.db = Database.getInstance();
    }

    @Override
    public Participant save(Participant participant) {
        String login = participant.getLogin();
        String companyKey = participant.getCompanyName();

        if (db.getParticipantsByLogin().containsKey(login)) {
            throw new DuplicateLoginException(login);
        }
        if (db.getParticipantsByCompany().containsKey(companyKey)) {
            throw new DuplicateCompanyException(participant.getCompanyName());
        }

        db.getParticipantsByLogin().put(login, participant);
        db.getParticipantsByCompany().put(companyKey, participant);
        return participant;
    }

    @Override
    public Participant findByLogin(String login) {
        Participant participant = db.getParticipantsByLogin().get(login);
        if (participant == null) {
            throw new NotFoundException(login);
        }
        return participant;
    }

    @Override
    public Participant findByCompanyName(String companyName) {
        Participant participant = db.getParticipantsByCompany().get(companyName);
        if (participant == null) {
            throw new NotFoundException(companyName);
        }
        return participant;
    }

    @Override
    public void delete(String login) {
        Participant participant = db.getParticipantsByLogin().get(login);
        if (participant == null) {
            throw new NotFoundException(login);
        }
        db.getParticipantsByCompany().remove(participant.getCompanyName());
        db.getParticipantsByLogin().remove(login);
    }

    @Override
    public boolean existsByCompanyName(String companyName) {
        return db.getParticipantsByCompany().containsKey(companyName);
    }
}
