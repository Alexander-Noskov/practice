package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.daoimpl.InMemoryApplicationDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryExpertDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryParticipantDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryRatingDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryTokenDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryUserDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.dto.request.CreateApplicationRequest;
import academy.tochkavhoda.competition.dto.request.LoginRequest;
import academy.tochkavhoda.competition.dto.request.RegisterExpertRequest;
import academy.tochkavhoda.competition.dto.request.RegisterParticipantRequest;
import academy.tochkavhoda.competition.exception.AccessDeniedException;
import academy.tochkavhoda.competition.exception.AuthException;
import academy.tochkavhoda.competition.exception.DuplicateCompanyException;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Application;
import academy.tochkavhoda.competition.model.Expert;
import academy.tochkavhoda.competition.model.Participant;
import academy.tochkavhoda.competition.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private InMemoryUserDao userDao;
    private InMemoryParticipantDao participantDao;
    private InMemoryExpertDao expertDao;
    private InMemoryApplicationDao applicationDao;
    private InMemoryRatingDao ratingDao;
    private TokenService tokenService;
    private ParticipantService participantService;
    private ExpertService expertService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Database.getInstance().getUsersByLogin().clear();
        Database.getInstance().getParticipantsByCompany().clear();
        Database.getInstance().getParticipantsByLogin().clear();
        Database.getInstance().getExpertsByLogin().clear();
        Database.getInstance().getApplicationsByDirection().clear();
        Database.getInstance().getApplicationsById().clear();
        Database.getInstance().getApplicationsByOwner().clear();
        Database.getInstance().getRatingsByApplication().clear();
        Database.getInstance().getTokens().clear();
        Database.getInstance().getTokenByLogin().clear();

        userDao = new InMemoryUserDao();
        participantDao = new InMemoryParticipantDao();
        expertDao = new InMemoryExpertDao();
        applicationDao = new InMemoryApplicationDao();
        ratingDao = new InMemoryRatingDao();
        InMemoryTokenDao tokenDao = new InMemoryTokenDao();

        tokenService = new TokenService(tokenDao);
        participantService = new ParticipantService(applicationDao, ratingDao);
        expertService = new ExpertService(expertDao, applicationDao, ratingDao);
        authService = new AuthService(userDao, participantDao, expertDao, tokenService,
                participantService, expertService);
    }

    private RegisterParticipantRequest participantRequest(String login, String password) {
        return new RegisterParticipantRequest("Иван", "Иванов", login, password, "Ромашка");
    }

    private RegisterExpertRequest expertRequest(String login, String password) {
        return new RegisterExpertRequest("Пётр", "Петров", login, password, Set.of("math"));
    }

    @Test
    void registerParticipantReturnsNonNullToken() {
        String token = authService.registerParticipant(participantRequest("ivan", "pass"));

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void registerParticipantStoresUserAndParticipant() {
        authService.registerParticipant(participantRequest("ivan", "pass"));

        User user = userDao.findByLogin("ivan");
        assertInstanceOf(Participant.class, user);
        assertEquals("Ромашка", ((Participant) user).getCompanyName());
        assertSame(user, participantDao.findByLogin("ivan"));
    }

    @Test
    void registerParticipantTokenIsUsableForAuthentication() {
        String token = authService.registerParticipant(participantRequest("ivan", "pass"));

        Participant participant = authService.authenticateParticipant(token);
        assertEquals("ivan", participant.getLogin());
    }

    @Test
    void registerParticipantThrowsDuplicateLoginForExistingLogin() {
        authService.registerParticipant(participantRequest("ivan", "pass"));

        assertThrows(DuplicateLoginException.class,
                () -> authService.registerParticipant(participantRequest("ivan", "other")));
    }

    @Test
    void registerParticipantThrowsDuplicateLoginWhenExpertWithSameLoginExists() {
        authService.registerExpert(expertRequest("ivan", "pass"));

        assertThrows(DuplicateLoginException.class,
                () -> authService.registerParticipant(participantRequest("ivan", "pass")));
    }

    @Test
    void registerParticipantThrowsDuplicateCompanyForExistingCompany() {
        authService.registerParticipant(participantRequest("ivan", "pass"));

        assertThrows(DuplicateCompanyException.class,
                () -> authService.registerParticipant(participantRequest("petr", "pass")));
    }

    @Test
    void registerParticipantStoresHashedPasswordNotPlain() {
        authService.registerParticipant(participantRequest("ivan", "secret"));

        User user = userDao.findByLogin("ivan");
        assertNotEquals("secret", user.getPasswordHash());
    }

    @Test
    void registerExpertReturnsNonNullToken() {
        String token = authService.registerExpert(expertRequest("expert1", "pass"));

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void registerExpertStoresUserAndExpert() {
        authService.registerExpert(expertRequest("expert1", "pass"));

        User user = userDao.findByLogin("expert1");
        assertInstanceOf(Expert.class, user);
        assertSame(user, expertDao.findByLogin("expert1"));
    }

    @Test
    void registerExpertTokenIsUsableForAuthentication() {
        String token = authService.registerExpert(expertRequest("expert1", "pass"));

        Expert expert = authService.authenticateExpert(token);
        assertEquals("expert1", expert.getLogin());
    }

    @Test
    void registerExpertThrowsDuplicateLoginForExistingLogin() {
        authService.registerExpert(expertRequest("expert1", "pass"));

        assertThrows(DuplicateLoginException.class,
                () -> authService.registerExpert(expertRequest("expert1", "other")));
    }

    @Test
    void registerExpertThrowsDuplicateLoginWhenParticipantWithSameLoginExists() {
        authService.registerParticipant(participantRequest("ivan", "pass"));

        assertThrows(DuplicateLoginException.class,
                () -> authService.registerExpert(expertRequest("ivan", "pass")));
    }

    @Test
    void registerParticipantWithDuplicateCompanyDoesNotLeaveUserInUsersByLogin() {
        authService.registerParticipant(participantRequest("ivan", "pass"));

        assertThrows(DuplicateCompanyException.class,
                () -> authService.registerParticipant(participantRequest("petr", "pass")));

        assertThrows(NotFoundException.class, () -> userDao.findByLogin("petr"));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        authService.registerParticipant(participantRequest("ivan", "pass"));

        String token = authService.login(new LoginRequest("ivan", "pass"));

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void loginTokenIsUsableForAuthentication() {
        authService.registerParticipant(participantRequest("ivan", "pass"));

        String token = authService.login(new LoginRequest("ivan", "pass"));

        assertEquals("ivan", authService.authenticate(token).getLogin());
    }

    @Test
    void loginThrowsNotFoundForUnknownLogin() {
        assertThrows(NotFoundException.class,
                () -> authService.login(new LoginRequest("unknown", "pass")));
    }

    @Test
    void loginThrowsAuthExceptionForWrongPassword() {
        authService.registerParticipant(participantRequest("ivan", "pass"));

        assertThrows(AuthException.class,
                () -> authService.login(new LoginRequest("ivan", "wrong")));
    }

    @Test
    void loginInvalidatesPreviousToken() {
        String firstToken = authService.registerParticipant(participantRequest("ivan", "pass"));
        String secondToken = authService.login(new LoginRequest("ivan", "pass"));

        assertNotEquals(firstToken, secondToken);
        assertThrows(NotFoundException.class, () -> authService.authenticate(firstToken));
        assertEquals("ivan", authService.authenticate(secondToken).getLogin());
    }

    @Test
    void logoutInvalidatesToken() {
        String token = authService.registerParticipant(participantRequest("ivan", "pass"));

        authService.logout(token);

        assertThrows(NotFoundException.class, () -> authService.authenticate(token));
    }

    @Test
    void logoutReturnsEmptyString() {
        String token = authService.registerParticipant(participantRequest("ivan", "pass"));

        assertEquals("", authService.logout(token));
    }

    @Test
    void logoutIsIdempotentForUnknownToken() {
        assertDoesNotThrow(() -> authService.logout("unknown"));
    }

    @Test
    void leaveRemovesParticipantAndAllData() {
        String token = authService.registerParticipant(participantRequest("ivan", "pass"));
        Participant participant = authService.authenticateParticipant(token);

        Application app = participantService.createApplication("ivan",
                new academy.tochkavhoda.competition.dto.request.CreateApplicationRequest(
                        "t", "d", Set.of("math"), 100L));
        ratingDao.save("expert1", app.getId(), 5);

        authService.leave(token);

        assertThrows(NotFoundException.class, () -> userDao.findByLogin("ivan"));
        assertThrows(NotFoundException.class, () -> participantDao.findByLogin("ivan"));
        assertThrows(NotFoundException.class, () -> applicationDao.findById(app.getId()));
        assertTrue(ratingDao.findByApplication(app.getId()).isEmpty());
        assertThrows(NotFoundException.class, () -> authService.authenticate(token));
    }

    @Test
    void leaveReleasesCompanyForNewRegistration() {
        String token = authService.registerParticipant(participantRequest("ivan", "pass"));
        authService.leave(token);

        assertDoesNotThrow(() ->
                authService.registerParticipant(participantRequest("petr", "pass")));
    }


    @Test
    void leaveRemovesExpertAndAllRatings() {
        String expertToken = authService.registerExpert(expertRequest("expert1", "pass"));
        authService.registerParticipant(participantRequest("ivan", "pass"));

        Application app = participantService.createApplication("ivan",
                new CreateApplicationRequest("t", "d", Set.of("math"), 100L));
        ratingDao.save("expert1", app.getId(), 5);

        authService.leave(expertToken);

        assertThrows(NotFoundException.class, () -> userDao.findByLogin("expert1"));
        assertThrows(NotFoundException.class, () -> expertDao.findByLogin("expert1"));
        assertTrue(ratingDao.findByApplication(app.getId()).isEmpty());
    }


    @Test
    void authenticateThrowsNotFoundForUnknownToken() {
        assertThrows(NotFoundException.class, () -> authService.authenticate("unknown"));
    }

    @Test
    void authenticateParticipantThrowsAccessDeniedForExpert() {
        String token = authService.registerExpert(expertRequest("expert1", "pass"));

        assertThrows(AccessDeniedException.class,
                () -> authService.authenticateParticipant(token));
    }

    @Test
    void authenticateExpertThrowsAccessDeniedForParticipant() {
        String token = authService.registerParticipant(participantRequest("ivan", "pass"));

        assertThrows(AccessDeniedException.class,
                () -> authService.authenticateExpert(token));
    }

    @Test
    void authenticateParticipantReturnsParticipant() {
        String token = authService.registerParticipant(participantRequest("ivan", "pass"));

        Participant participant = authService.authenticateParticipant(token);

        assertEquals("ivan", participant.getLogin());
        assertEquals("Ромашка", participant.getCompanyName());
    }

    @Test
    void authenticateExpertReturnsExpert() {
        String token = authService.registerExpert(expertRequest("expert1", "pass"));

        Expert expert = authService.authenticateExpert(token);

        assertEquals("expert1", expert.getLogin());
        assertEquals(Set.of("math"), expert.getDirections());
    }
}