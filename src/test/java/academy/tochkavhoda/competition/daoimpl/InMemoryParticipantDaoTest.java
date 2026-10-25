package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.DuplicateCompanyException;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryParticipantDaoTest {
    private InMemoryParticipantDao participantDao;

    @BeforeEach
    void setUp() {
        Database.getInstance().getParticipantsByLogin().clear();
        Database.getInstance().getParticipantsByCompany().clear();
        participantDao = new InMemoryParticipantDao();
    }

    private Participant createParticipant(String login, String companyName) {
        return new Participant(login, "hash", "Иван", "Иванов", companyName);
    }

    @Test
    void saveReturnsSameParticipantAndStoresIt() {
        Participant participant = createParticipant("ivan", "Ромашка");

        Participant saved = participantDao.save(participant);

        assertSame(participant, saved);
        assertSame(participant, participantDao.findByLogin("ivan"));
        assertSame(participant, participantDao.findByCompanyName("Ромашка"));
    }

    @Test
    void saveThrowsDuplicateLoginExceptionForExistingLogin() {
        participantDao.save(createParticipant("ivan", "Ромашка"));

        assertThrows(DuplicateLoginException.class,
                () -> participantDao.save(createParticipant("ivan", "Лютик")));
    }

    @Test
    void saveThrowsDuplicateCompanyExceptionForExistingCompany() {
        participantDao.save(createParticipant("ivan", "Ромашка"));

        assertThrows(DuplicateCompanyException.class,
                () -> participantDao.save(createParticipant("petr", "Ромашка")));
    }

    @Test
    void saveKeepsOriginalParticipantOnDuplicateLogin() {
        Participant first = createParticipant("ivan", "Ромашка");
        Participant second = createParticipant("ivan", "Лютик");
        participantDao.save(first);

        assertThrows(DuplicateLoginException.class,
                () -> participantDao.save(second));

        assertSame(first, participantDao.findByLogin("ivan"));
        assertThrows(NotFoundException.class,
                () -> participantDao.findByCompanyName("Лютик"));
    }

    @Test
    void saveKeepsOriginalParticipantOnDuplicateCompany() {
        Participant first = createParticipant("ivan", "Ромашка");
        Participant second = createParticipant("petr", "Ромашка");
        participantDao.save(first);

        assertThrows(DuplicateCompanyException.class,
                () -> participantDao.save(second));

        assertSame(first, participantDao.findByCompanyName("Ромашка"));
        assertThrows(NotFoundException.class,
                () -> participantDao.findByLogin("petr"));
    }

    @Test
    void saveStoresParticipantsWithDifferentLoginsAndCompanies() {
        Participant a = createParticipant("ivan", "Ромашка");
        Participant b = createParticipant("petr", "Лютик");

        participantDao.save(a);
        participantDao.save(b);

        assertSame(a, participantDao.findByLogin("ivan"));
        assertSame(b, participantDao.findByLogin("petr"));
        assertSame(a, participantDao.findByCompanyName("Ромашка"));
        assertSame(b, participantDao.findByCompanyName("Лютик"));
    }

    @Test
    void findByLoginReturnsStoredParticipant() {
        Participant participant = createParticipant("ivan", "Ромашка");
        participantDao.save(participant);

        assertSame(participant, participantDao.findByLogin("ivan"));
    }

    @Test
    void findByLoginThrowsNotFoundExceptionForUnknownLogin() {
        assertThrows(NotFoundException.class,
                () -> participantDao.findByLogin("unknown"));
    }

    @Test
    void findByCompanyNameReturnsStoredParticipant() {
        Participant participant = createParticipant("ivan", "Ромашка");
        participantDao.save(participant);

        assertSame(participant, participantDao.findByCompanyName("Ромашка"));
    }

    @Test
    void findByCompanyNameThrowsNotFoundExceptionForUnknownCompany() {
        assertThrows(NotFoundException.class,
                () -> participantDao.findByCompanyName("Неизвестная"));
    }

    @Test
    void deleteRemovesStoredParticipant() {
        participantDao.save(createParticipant("ivan", "Ромашка"));

        participantDao.delete("ivan");

        assertThrows(NotFoundException.class,
                () -> participantDao.findByLogin("ivan"));
        assertThrows(NotFoundException.class,
                () -> participantDao.findByCompanyName("Ромашка"));
    }

    @Test
    void deleteThrowsNotFoundExceptionForUnknownLogin() {
        assertThrows(NotFoundException.class,
                () -> participantDao.delete("unknown"));
    }

    @Test
    void deleteReleasesCompanyForNewRegistration() {
        participantDao.save(createParticipant("ivan", "Ромашка"));
        participantDao.delete("ivan");

        Participant newParticipant = createParticipant("petr", "Ромашка");

        assertDoesNotThrow(() -> participantDao.save(newParticipant));
        assertSame(newParticipant, participantDao.findByCompanyName("Ромашка"));
    }

    @Test
    void deleteAllowsSavingSameLoginAgain() {
        participantDao.save(createParticipant("ivan", "Ромашка"));
        participantDao.delete("ivan");

        Participant again = createParticipant("ivan", "Лютик");
        participantDao.save(again);

        assertSame(again, participantDao.findByLogin("ivan"));
    }

}