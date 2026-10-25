package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserDaoTest {
    private InMemoryUserDao userDao;

    @BeforeEach
    void setUp() {
        Database.getInstance().getUsersByLogin().clear();
        userDao = new InMemoryUserDao();
    }


    private User createUser(String login) {
        return new User(login, "hash", "Иван", "Иванов");
    }

    @Test
    void saveReturnsSameUserAndStoresIt() {
        User user = createUser("ivan");

        User saved = userDao.save(user);

        assertSame(user, saved);
        assertSame(user, userDao.findByLogin("ivan"));
    }

    @Test
    void saveThrowsDuplicateLoginExceptionForExistingLogin() {
        userDao.save(createUser("ivan"));

        assertThrows(DuplicateLoginException.class,
                () -> userDao.save(createUser("ivan")));
    }

    @Test
    void saveKeepsOriginalUserOnDuplicateLogin() {
        User first = createUser("ivan");
        User second = createUser("ivan");
        userDao.save(first);

        assertThrows(DuplicateLoginException.class, () -> userDao.save(second));

        assertSame(first, userDao.findByLogin("ivan"));
    }

    @Test
    void saveStoresUsersWithDifferentLogins() {
        User a = createUser("ivan");
        User b = createUser("petr");

        userDao.save(a);
        userDao.save(b);

        assertSame(a, userDao.findByLogin("ivan"));
        assertSame(b, userDao.findByLogin("petr"));
    }

    @Test
    void findByLoginReturnsStoredUser() {
        User user = createUser("ivan");
        userDao.save(user);

        assertSame(user, userDao.findByLogin("ivan"));
    }

    @Test
    void findByLoginThrowsNotFoundExceptionForUnknownLogin() {
        assertThrows(NotFoundException.class,
                () -> userDao.findByLogin("unknown"));
    }

    @Test
    void deleteRemovesStoredUser() {
        userDao.save(createUser("ivan"));

        userDao.delete("ivan");

        assertThrows(NotFoundException.class, () -> userDao.findByLogin("ivan"));
    }

    @Test
    void deleteThrowsNotFoundExceptionForUnknownLogin() {
        assertThrows(NotFoundException.class,
                () -> userDao.delete("unknown"));
    }

    @Test
    void deleteAllowsSavingSameLoginAgain() {
        userDao.save(createUser("ivan"));
        userDao.delete("ivan");

        User again = createUser("ivan");
        userDao.save(again);

        assertSame(again, userDao.findByLogin("ivan"));
    }
}