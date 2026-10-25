package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.daoimpl.InMemoryApplicationDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryRatingDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.dto.response.GrantResult;
import academy.tochkavhoda.competition.model.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GrantServiceTest {
    private InMemoryApplicationDao applicationDao;
    private InMemoryRatingDao ratingDao;
    private GrantService grantService;

    @BeforeEach
    void setUp() {
        Database.getInstance().getRatingsByApplication().clear();
        Database.getInstance().getApplicationsByOwner().clear();
        Database.getInstance().getApplicationsById().clear();
        Database.getInstance().getApplicationsByDirection().clear();
        applicationDao = new InMemoryApplicationDao();
        ratingDao = new InMemoryRatingDao();
        grantService = new GrantService(applicationDao, ratingDao);
    }

    private Application createApplication(String owner, long amount, String... directions) {
        Application app = new Application(UUID.randomUUID(), owner, "t", "d",
                Set.of(directions), amount);
        applicationDao.save(app);
        return app;
    }

    private void rate(Application app, String expertLogin, int value) {
        ratingDao.save(expertLogin, app.getId(), value);
    }

    @Test
    void rankApplicationsReturnsEmptyForNoApplications() {
        assertTrue(grantService.rankApplications().isEmpty());
    }

    @Test
    void rankApplicationsExcludesApplicationsWithoutRatings() {
        Application rated = createApplication("ivan", 100, "math");
        createApplication("petr", 200, "math");
        rate(rated, "expert1", 5);

        List<Application> ranked = grantService.rankApplications();

        assertEquals(1, ranked.size());
        assertSame(rated, ranked.get(0));
    }

    @Test
    void rankApplicationsOrdersByAverageDescending() {
        Application low = createApplication("ivan", 100, "math");
        Application high = createApplication("petr", 100, "math");
        Application mid = createApplication("sergey", 100, "math");
        rate(low, "e1", 2);
        rate(high, "e1", 5);
        rate(mid, "e1", 3);

        List<Application> ranked = grantService.rankApplications();

        assertEquals(List.of(high, mid, low), ranked);
    }

    @Test
    void rankApplicationsOrdersByAverageAcrossMultipleExperts() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 100, "math");
        rate(a, "e1", 5);
        rate(a, "e2", 5);
        rate(b, "e1", 5);
        rate(b, "e2", 3);

        List<Application> ranked = grantService.rankApplications();

        assertEquals(List.of(a, b), ranked);
    }

    @Test
    void rankApplicationsOrdersByAmountAscendingWhenAverageEqual() {
        Application expensive = createApplication("ivan", 500, "math");
        Application cheap = createApplication("petr", 100, "math");
        Application middle = createApplication("sergey", 300, "math");
        rate(expensive, "e1", 5);
        rate(cheap, "e1", 5);
        rate(middle, "e1", 5);

        List<Application> ranked = grantService.rankApplications();

        assertEquals(List.of(cheap, middle, expensive), ranked);
    }

    @Test
    void rankApplicationsCombinesAverageAndAmount() {
        Application a = createApplication("ivan", 300, "math");
        Application b = createApplication("petr", 100, "math");
        Application c = createApplication("sergey", 50, "math");
        rate(a, "e1", 5);
        rate(b, "e1", 5);
        rate(c, "e1", 4);

        List<Application> ranked = grantService.rankApplications();

        assertEquals(List.of(b, a, c), ranked);
    }


    @Test
    void distributeEmptyWhenNoApplications() {
        GrantResult result = grantService.distribute(1000, 0);

        assertTrue(result.funded().isEmpty());
        assertTrue(result.skippedByFund().isEmpty());
        assertTrue(result.skippedByThreshold().isEmpty());
        assertEquals(1000, result.remainingFund());
    }

    @Test
    void distributeFundsTopApplication() {
        Application a = createApplication("ivan", 300, "math");
        rate(a, "e1", 5);

        GrantResult result = grantService.distribute(1000, 0);

        assertEquals(List.of(a), result.funded());
        assertEquals(700, result.remainingFund());
    }

    @Test
    void distributeFundsSeveralApplicationsInRankOrder() {
        Application a = createApplication("ivan", 100, "math");   // средняя 5
        Application b = createApplication("petr", 200, "math");   // средняя 4
        Application c = createApplication("sergey", 300, "math"); // средняя 3
        rate(a, "e1", 5);
        rate(b, "e1", 4);
        rate(c, "e1", 3);

        GrantResult result = grantService.distribute(600, 0);

        assertEquals(List.of(a, b, c), result.funded());
        assertEquals(0, result.remainingFund());
    }

    @Test
    void distributeSkipsApplicationThatDoesNotFitFundButContinues() {
        Application a = createApplication("ivan", 700, "math");
        Application b = createApplication("petr", 100, "math");
        rate(a, "e1", 5);
        rate(b, "e1", 4);

        GrantResult result = grantService.distribute(500, 0);

        assertEquals(List.of(b), result.funded());
        assertEquals(List.of(a), result.skippedByFund());
        assertEquals(400, result.remainingFund());
    }

    @Test
    void distributeStopsAtThreshold() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 100, "math");
        Application c = createApplication("sergey", 100, "math");
        rate(a, "e1", 5);
        rate(b, "e1", 3);
        rate(c, "e1", 2);

        GrantResult result = grantService.distribute(1000, 3.5);

        assertEquals(List.of(a), result.funded());
        assertEquals(List.of(b, c), result.skippedByThreshold());
        assertEquals(900, result.remainingFund());
    }

    @Test
    void distributeThresholdStopsEvenIfFundAllows() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 100, "math");
        rate(a, "e1", 5);
        rate(b, "e1", 2);

        GrantResult result = grantService.distribute(1000, 4.0);

        assertEquals(List.of(a), result.funded());
        assertEquals(List.of(b), result.skippedByThreshold());
        assertEquals(900, result.remainingFund());
    }

    @Test
    void distributeZeroThresholdFundsOnlyAboveZero() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 100, "math");
        rate(a, "e1", 5);
        rate(b, "e1", 1);

        GrantResult result = grantService.distribute(1000, 0);

        assertEquals(2, result.funded().size());
        assertEquals(800, result.remainingFund());
    }

    @Test
    void distributeIgnoresApplicationsWithoutRatings() {
        Application rated = createApplication("ivan", 100, "math");
        createApplication("petr", 100, "math");   // без оценок
        rate(rated, "e1", 5);

        GrantResult result = grantService.distribute(1000, 0);

        assertEquals(1, result.funded().size());
        assertSame(rated, result.funded().get(0));
        assertEquals(900, result.remainingFund());
    }

    @Test
    void distributeReturnsRemainingFundWhenApplicationsExhausted() {
        Application a = createApplication("ivan", 100, "math");
        rate(a, "e1", 5);

        GrantResult result = grantService.distribute(1000, 0);

        assertEquals(900, result.remainingFund());
    }

    @Test
    void distributeThrowsForNegativeFund() {
        assertThrows(IllegalArgumentException.class,
                () -> grantService.distribute(-1, 0));
    }

    @Test
    void distributeSkipsAllWhenFundIsZero() {
        Application a = createApplication("ivan", 100, "math");
        rate(a, "e1", 5);

        GrantResult result = grantService.distribute(0, 0);

        assertTrue(result.funded().isEmpty());
        assertEquals(List.of(a), result.skippedByFund());
        assertEquals(0, result.remainingFund());
    }
}