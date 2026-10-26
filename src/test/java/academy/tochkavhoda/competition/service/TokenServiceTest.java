package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.daoimpl.InMemoryTokenDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.AuthException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    private InMemoryTokenDao tokenDao;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        Database.getInstance().getTokenByLogin().clear();
        Database.getInstance().getTokens().clear();
        tokenDao = new InMemoryTokenDao();
        tokenService = new TokenService(tokenDao);
    }

    @Test
    void issueReturnsNonNullToken() {
        String token = tokenService.issue("ivan");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void issueStoresTokenForLogin() {
        String token = tokenService.issue("ivan");

        assertEquals("ivan", tokenDao.findLoginByToken(token));
    }

    @Test
    void issueReturnsDifferentTokensForDifferentCalls() {
        String t1 = tokenService.issue("ivan");
        String t2 = tokenService.issue("petr");

        assertNotEquals(t1, t2);
    }

    @Test
    void issueInvalidatesPreviousTokenForSameLogin() {
        String oldToken = tokenService.issue("ivan");
        String newToken = tokenService.issue("ivan");

        assertNotEquals(oldToken, newToken);
        assertEquals("ivan", tokenDao.findLoginByToken(newToken));
        assertThrows(AuthException.class,
                () -> tokenDao.findLoginByToken(oldToken));
    }

    @Test
    void issueDoesNotAffectTokensOfOtherLogins() {
        String ivanToken = tokenService.issue("ivan");
        String petrToken = tokenService.issue("petr");

        String newIvanToken = tokenService.issue("ivan");

        assertEquals("petr", tokenDao.findLoginByToken(petrToken));
        assertNotEquals(ivanToken, newIvanToken);
    }

    @Test
    void authenticateReturnsLoginForValidToken() {
        String token = tokenService.issue("ivan");

        assertEquals("ivan", tokenService.authenticate(token));
    }

    @Test
    void authenticateThrowsNotFoundExceptionForUnknownToken() {
        assertThrows(AuthException.class,
                () -> tokenService.authenticate("unknown"));
    }

    @Test
    void authenticateThrowsNotFoundExceptionAfterLogout() {
        String token = tokenService.issue("ivan");
        tokenService.logout(token);

        assertThrows(AuthException.class,
                () -> tokenService.authenticate(token));
    }

    @Test
    void logoutInvalidatesToken() {
        String token = tokenService.issue("ivan");

        tokenService.logout(token);

        assertThrows(AuthException.class,
                () -> tokenService.authenticate(token));
    }

    @Test
    void logoutDoesNotAffectOtherTokens() {
        String ivanToken = tokenService.issue("ivan");
        String petrToken = tokenService.issue("petr");

        tokenService.logout(ivanToken);

        assertEquals("petr", tokenService.authenticate(petrToken));
        assertThrows(AuthException.class,
                () -> tokenService.authenticate(ivanToken));
    }

    @Test
    void logoutIsIdempotentForUnknownToken() {
        assertDoesNotThrow(() -> tokenService.logout("unknown"));
    }

    @Test
    void logoutAllowsIssuingNewTokenForSameLogin() {
        String oldToken = tokenService.issue("ivan");
        tokenService.logout(oldToken);

        String newToken = tokenService.issue("ivan");

        assertEquals("ivan", tokenService.authenticate(newToken));
        assertThrows(AuthException.class,
                () -> tokenService.authenticate(oldToken));
    }

    @Test
    void revokeByLoginInvalidatesTokenOfLogin() {
        String token = tokenService.issue("ivan");

        tokenService.revokeByLogin("ivan");

        assertThrows(AuthException.class,
                () -> tokenService.authenticate(token));
    }

    @Test
    void revokeByLoginDoesNotAffectOtherLogins() {
        String ivanToken = tokenService.issue("ivan");
        String petrToken = tokenService.issue("petr");

        tokenService.revokeByLogin("ivan");

        assertEquals("petr", tokenService.authenticate(petrToken));
        assertThrows(AuthException.class,
                () -> tokenService.authenticate(ivanToken));
    }

    @Test
    void revokeByLoginIsNoOpForLoginWithoutToken() {
        assertDoesNotThrow(() -> tokenService.revokeByLogin("unknown"));
    }

}