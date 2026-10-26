package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.AuthException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTokenDaoTest {
    private InMemoryTokenDao tokenDao;

    @BeforeEach
    void setUp() {
        Database.getInstance().getTokens().clear();
        Database.getInstance().getTokenByLogin().clear();
        tokenDao = new InMemoryTokenDao();
    }


    @Test
    void saveStoresTokenForLogin() {
        tokenDao.save("token-1", "ivan");

        assertEquals("ivan", tokenDao.findLoginByToken("token-1"));
    }

    @Test
    void saveOverwritesPreviousTokenForSameLogin() {
        tokenDao.save("token-1", "ivan");
        tokenDao.save("token-2", "ivan");

        assertEquals("ivan", tokenDao.findLoginByToken("token-2"));
    }

    @Test
    void saveStoresTokensForDifferentLoginsIndependently() {
        tokenDao.save("token-1", "ivan");
        tokenDao.save("token-2", "petr");

        assertEquals("ivan", tokenDao.findLoginByToken("token-1"));
        assertEquals("petr", tokenDao.findLoginByToken("token-2"));
    }

    @Test
    void findLoginByTokenReturnsStoredLogin() {
        tokenDao.save("token-1", "ivan");

        assertEquals("ivan", tokenDao.findLoginByToken("token-1"));
    }

    @Test
    void findLoginByTokenThrowsNotFoundExceptionForUnknownToken() {
        assertThrows(AuthException.class,
                () -> tokenDao.findLoginByToken("unknown"));
    }

    @Test
    void deleteRemovesToken() {
        tokenDao.save("token-1", "ivan");

        tokenDao.delete("token-1");

        assertThrows(AuthException.class,
                () -> tokenDao.findLoginByToken("token-1"));
    }

    @Test
    void deleteDoesNothingForUnknownToken() {
        assertDoesNotThrow(() -> tokenDao.delete("unknown"));
    }

    @Test
    void deleteKeepsTokensOfOtherLogins() {
        tokenDao.save("token-1", "ivan");
        tokenDao.save("token-2", "petr");

        tokenDao.delete("token-1");

        assertEquals("petr", tokenDao.findLoginByToken("token-2"));
        assertThrows(AuthException.class,
                () -> tokenDao.findLoginByToken("token-1"));
    }

    @Test
    void deleteAllowsIssuingNewTokenForSameLogin() {
        tokenDao.save("token-1", "ivan");
        tokenDao.delete("token-1");

        tokenDao.save("token-2", "ivan");

        assertEquals("ivan", tokenDao.findLoginByToken("token-2"));
        assertThrows(AuthException.class,
                () -> tokenDao.findLoginByToken("token-1"));
    }
}