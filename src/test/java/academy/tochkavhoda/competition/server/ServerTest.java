package academy.tochkavhoda.competition.server;

import academy.tochkavhoda.competition.dao.ApplicationDao;
import academy.tochkavhoda.competition.dao.ExpertDao;
import academy.tochkavhoda.competition.dao.ParticipantDao;
import academy.tochkavhoda.competition.dao.RatingDao;
import academy.tochkavhoda.competition.dao.TokenDao;
import academy.tochkavhoda.competition.dao.UserDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryApplicationDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryExpertDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryParticipantDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryRatingDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryTokenDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryUserDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.dto.response.TokenDto;
import academy.tochkavhoda.competition.server.dto.ServerResponse;
import academy.tochkavhoda.competition.service.AuthService;
import academy.tochkavhoda.competition.service.ExpertService;
import academy.tochkavhoda.competition.service.GrantService;
import academy.tochkavhoda.competition.service.ParticipantService;
import academy.tochkavhoda.competition.service.TokenService;
import academy.tochkavhoda.competition.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    private Server server;

    @BeforeEach
    void setUp() {
        Database.getInstance().getUsersByLogin().clear();
        Database.getInstance().getParticipantsByLogin().clear();
        Database.getInstance().getParticipantsByCompany().clear();
        Database.getInstance().getExpertsByLogin().clear();
        Database.getInstance().getApplicationsByDirection().clear();
        Database.getInstance().getApplicationsById().clear();
        Database.getInstance().getApplicationsByOwner().clear();
        Database.getInstance().getRatingsByApplication().clear();
        Database.getInstance().getTokens().clear();
        Database.getInstance().getTokenByLogin().clear();

        UserDao userDao = new InMemoryUserDao();
        ParticipantDao participantDao = new InMemoryParticipantDao();
        ExpertDao expertDao = new InMemoryExpertDao();
        ApplicationDao applicationDao = new InMemoryApplicationDao();
        RatingDao ratingDao = new InMemoryRatingDao();
        TokenDao tokenDao = new InMemoryTokenDao();

        TokenService tokenService = new TokenService(tokenDao);
        ParticipantService participantService = new ParticipantService(applicationDao, ratingDao);
        ExpertService expertService = new ExpertService(expertDao, applicationDao, ratingDao);
        AuthService authService = new AuthService(userDao, participantDao, expertDao,
                tokenService, participantService, expertService);
        GrantService grantService = new GrantService(applicationDao, ratingDao);

        server = new Server(authService, participantService, expertService, grantService);
    }

    private String participantJson(String login, String password, String company) {
        return """
                {"firstName":"Иван","lastName":"Иванов","login":"%s",
                 "password":"%s","companyName":"%s"}
                """.formatted(login, password, company);
    }

    private String expertJson(String login, String password, String... directions) {
        String dirs = String.join("\",\"", directions);
        return """
                {"firstName":"Пётр","lastName":"Петров","login":"%s",
                 "password":"%s","directions":["%s"]}
                """.formatted(login, password, dirs);
    }

    private String tokenOf(ServerResponse response) {
        return JsonUtil.fromJson(response.responseData(), TokenDto.class).token();
    }

    private ServerResponse registerParticipant(String login, String company) {
        return server.registerParticipant(participantJson(login, "pass", company));
    }

    private ServerResponse registerExpert(String login, String... directions) {
        return server.registerExpert(expertJson(login, "pass", directions));
    }

    @Test
    void registerParticipantReturns200AndToken() {
        ServerResponse response = registerParticipant("ivan", "Ромашка");

        assertEquals(200, response.responseCode());
        assertNotNull(tokenOf(response));
    }

    @Test
    void registerParticipantReturns400ForDuplicateLogin() {
        registerParticipant("ivan", "Ромашка");

        ServerResponse response = registerParticipant("ivan", "Лютик");

        assertEquals(400, response.responseCode());
        assertTrue(response.responseData().contains("error"));
    }

    @Test
    void registerParticipantReturns400ForDuplicateCompany() {
        registerParticipant("ivan", "Ромашка");

        ServerResponse response = registerParticipant("petr", "Ромашка");

        assertEquals(400, response.responseCode());
    }

    @Test
    void registerParticipantReturns400ForInvalidJson() {
        ServerResponse response = server.registerParticipant("{not json}");

        assertEquals(400, response.responseCode());
    }

    @Test
    void registerExpertReturns200AndToken() {
        ServerResponse response = registerExpert("expert1", "math");

        assertEquals(200, response.responseCode());
        assertNotNull(tokenOf(response));
    }

    @Test
    void registerExpertReturns400ForDuplicateLogin() {
        registerExpert("expert1", "math");

        ServerResponse response = registerExpert("expert1", "physics");

        assertEquals(400, response.responseCode());
    }

    @Test
    void loginReturns200AndTokenForValidCredentials() {
        registerParticipant("ivan", "Ромашка");

        ServerResponse response = server.login("""
                {"login":"ivan","password":"pass"}
                """);

        assertEquals(200, response.responseCode());
        assertNotNull(tokenOf(response));
    }

    @Test
    void loginReturns401ForWrongPassword() {
        registerParticipant("ivan", "Ромашка");

        ServerResponse response = server.login("""
                {"login":"ivan","password":"wrong"}
                """);

        assertEquals(401, response.responseCode());
    }

    @Test
    void loginReturns400ForUnknownLogin() {
        ServerResponse response = server.login("""
                {"login":"unknown","password":"pass"}
                """);

        assertEquals(400, response.responseCode());
    }


    @Test
    void logoutReturns200AndInvalidatesToken() {
        String token = tokenOf(registerParticipant("ivan", "Ромашка"));

        ServerResponse response = server.logout(token);

        assertEquals(200, response.responseCode());

        ServerResponse afterLogout = server.getMyApplications(token);
        assertEquals(401, afterLogout.responseCode());
    }

    @Test
    void logoutReturns401ForUnknownToken() {
        ServerResponse response = server.logout("unknown");

        assertEquals(401, response.responseCode());
    }

    @Test
    void leaveReturns200AndRemovesUser() {
        String token = tokenOf(registerParticipant("ivan", "Ромашка"));

        ServerResponse response = server.leave(token);

        assertEquals(200, response.responseCode());

        ServerResponse login = server.login("""
                {"login":"ivan","password":"pass"}
                """);
        assertEquals(400, login.responseCode());
    }

    @Test
    void createApplicationReturns200AndApplication() {
        String token = tokenOf(registerParticipant("ivan", "Ромашка"));

        ServerResponse response = server.createApplication(token, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);

        assertEquals(200, response.responseCode());
        assertTrue(response.responseData().contains("\"title\":\"t\""));
    }

    @Test
    void createApplicationReturns403ForExpertToken() {
        String token = tokenOf(registerExpert("expert1", "math"));

        ServerResponse response = server.createApplication(token, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);

        assertEquals(403, response.responseCode());
    }

    @Test
    void createApplicationReturns401ForUnknownToken() {
        ServerResponse response = server.createApplication("unknown", """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);

        assertEquals(401, response.responseCode());
    }

    @Test
    void createApplicationReturns400ForInvalidAmount() {
        String token = tokenOf(registerParticipant("ivan", "Ромашка"));

        ServerResponse response = server.createApplication(token, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":-5}
                """);

        assertEquals(400, response.responseCode());
    }

    @Test
    void getMyApplicationsReturns200AndList() {
        String token = tokenOf(registerParticipant("ivan", "Ромашка"));
        server.createApplication(token, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);

        ServerResponse response = server.getMyApplications(token);

        assertEquals(200, response.responseCode());
        assertTrue(response.responseData().contains("\"title\":\"t\""));
    }

    @Test
    void cancelApplicationReturns200AndRemovesApplication() {
        String token = tokenOf(registerParticipant("ivan", "Ромашка"));
        ServerResponse created = server.createApplication(token, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);
        String id = extractApplicationId(created.responseData());

        ServerResponse response = server.cancelApplication(token,
                "{\"applicationId\":\"" + id + "\"}");

        assertEquals(200, response.responseCode());
        ServerResponse list = server.getMyApplications(token);
        assertFalse(list.responseData().contains("\"title\":\"t\""));
    }

    @Test
    void rateReturns200ForExpert() {
        String participantToken = tokenOf(registerParticipant("ivan", "Ромашка"));
        String expertToken = tokenOf(registerExpert("expert1", "math"));

        ServerResponse created = server.createApplication(participantToken, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);
        String id = extractApplicationId(created.responseData());

        ServerResponse response = server.rate(expertToken,
                "{\"applicationId\":\"" + id + "\",\"value\":5}");

        assertEquals(200, response.responseCode());
    }

    @Test
    void rateReturns400ForInvalidValue() {
        String participantToken = tokenOf(registerParticipant("ivan", "Ромашка"));
        String expertToken = tokenOf(registerExpert("expert1", "math"));
        ServerResponse created = server.createApplication(participantToken, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);
        String id = extractApplicationId(created.responseData());

        ServerResponse response = server.rate(expertToken,
                "{\"applicationId\":\"" + id + "\",\"value\":6}");

        assertEquals(400, response.responseCode());
    }

    @Test
    void rateReturns403ForParticipantToken() {
        String participantToken = tokenOf(registerParticipant("ivan", "Ромашка"));

        ServerResponse response = server.rate(participantToken,
                "{\"applicationId\":\"" + java.util.UUID.randomUUID() + "\",\"value\":5}");

        assertEquals(403, response.responseCode());
    }

    @Test
    void getApplicationsByDirectionsReturns200AndFilteredList() {
        String participantToken = tokenOf(registerParticipant("ivan", "Ромашка"));
        String expertToken = tokenOf(registerExpert("expert1", "math"));
        server.createApplication(participantToken, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);

        ServerResponse response = server.getApplicationsByDirections(expertToken,
                "{\"directions\":[\"math\"]}");

        assertEquals(200, response.responseCode());
        assertTrue(response.responseData().contains("\"title\":\"t\""));
    }

    @Test
    void getRatedApplicationsReturns200AndRatedList() {
        String participantToken = tokenOf(registerParticipant("ivan", "Ромашка"));
        String expertToken = tokenOf(registerExpert("expert1", "math"));
        ServerResponse created = server.createApplication(participantToken, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);
        String id = extractApplicationId(created.responseData());
        server.rate(expertToken, "{\"applicationId\":\"" + id + "\",\"value\":5}");

        ServerResponse response = server.getRatedApplications(expertToken);

        assertEquals(200, response.responseCode());
        assertTrue(response.responseData().contains("\"title\":\"t\""));
    }

    @Test
    void distributeGrantsReturns200AndResult() {
        String participantToken = tokenOf(registerParticipant("ivan", "Ромашка"));
        String expertToken = tokenOf(registerExpert("expert1", "math"));
        ServerResponse created = server.createApplication(participantToken, """
                {"title":"t","description":"d","directions":["math"],"requestedAmount":100}
                """);
        String id = extractApplicationId(created.responseData());
        server.rate(expertToken, "{\"applicationId\":\"" + id + "\",\"value\":5}");

        ServerResponse response = server.distributeGrants("""
                {"totalFund":1000,"threshold":0}
                """);

        assertEquals(200, response.responseCode());
        assertTrue(response.responseData().contains("\"remainingFund\":900"));
    }

    private String extractApplicationId(String json) {
        return JsonUtil.fromJson(json, com.google.gson.JsonObject.class)
                .get("id").getAsString();
    }
}