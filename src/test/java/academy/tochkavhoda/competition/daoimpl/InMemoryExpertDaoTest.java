package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Expert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryExpertDaoTest {
    private InMemoryExpertDao expertDao;

    @BeforeEach
    void setUp() {
        Database.getInstance().getExpertsByLogin().clear();
        expertDao = new InMemoryExpertDao();
    }

    private Expert createExpert(String login) {
        return new Expert(login, "hash", "Пётр", "Петров", Set.of("математика", "информатика"));
    }

    @Test
    void saveReturnsSameExpertAndStoresIt() {
        Expert expert = createExpert("expert1");

        Expert saved = expertDao.save(expert);

        assertSame(expert, saved);
        assertSame(expert, expertDao.findByLogin("expert1"));
    }

    @Test
    void saveThrowsDuplicateLoginExceptionForExistingLogin() {
        expertDao.save(createExpert("expert1"));

        assertThrows(DuplicateLoginException.class,
                () -> expertDao.save(createExpert("expert1")));
    }

    @Test
    void saveKeepsOriginalExpertOnDuplicateLogin() {
        Expert first = createExpert("expert1");
        Expert second = new Expert("expert1", "other", "Иван", "Иванов", Set.of("физика"));
        expertDao.save(first);

        assertThrows(DuplicateLoginException.class, () -> expertDao.save(second));

        assertSame(first, expertDao.findByLogin("expert1"));
    }

    @Test
    void saveStoresExpertsWithDifferentLogins() {
        Expert a = createExpert("expert1");
        Expert b = createExpert("expert2");

        expertDao.save(a);
        expertDao.save(b);

        assertSame(a, expertDao.findByLogin("expert1"));
        assertSame(b, expertDao.findByLogin("expert2"));
    }

    @Test
    void findByLoginReturnsStoredExpert() {
        Expert expert = createExpert("expert1");
        expertDao.save(expert);

        assertSame(expert, expertDao.findByLogin("expert1"));
    }

    @Test
    void findByLoginThrowsNotFoundExceptionForUnknownLogin() {
        assertThrows(NotFoundException.class,
                () -> expertDao.findByLogin("unknown"));
    }

    @Test
    void deleteRemovesStoredExpert() {
        expertDao.save(createExpert("expert1"));

        expertDao.delete("expert1");

        assertThrows(NotFoundException.class,
                () -> expertDao.findByLogin("expert1"));
    }

    @Test
    void deleteThrowsNotFoundExceptionForUnknownLogin() {
        assertThrows(NotFoundException.class,
                () -> expertDao.delete("unknown"));
    }

    @Test
    void deleteAllowsSavingSameLoginAgain() {
        expertDao.save(createExpert("expert1"));
        expertDao.delete("expert1");

        Expert again = createExpert("expert1");
        expertDao.save(again);

        assertSame(again, expertDao.findByLogin("expert1"));
    }
}