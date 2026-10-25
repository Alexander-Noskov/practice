package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.daoimpl.InMemoryApplicationDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryExpertDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryRatingDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.InvalidRatingException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Application;
import academy.tochkavhoda.competition.model.Expert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExpertServiceTest {
    private InMemoryExpertDao expertDao;
    private InMemoryApplicationDao applicationDao;
    private InMemoryRatingDao ratingDao;
    private ExpertService expertService;

    @BeforeEach
    void setUp() {
        Database.getInstance().getApplicationsByOwner().clear();
        Database.getInstance().getApplicationsById().clear();
        Database.getInstance().getApplicationsByDirection().clear();
        Database.getInstance().getRatingsByApplication().clear();
        Database.getInstance().getExpertsByLogin().clear();
        Database.getInstance().getUsersByLogin().clear();
        expertDao = new InMemoryExpertDao();
        applicationDao = new InMemoryApplicationDao();
        ratingDao = new InMemoryRatingDao();
        expertService = new ExpertService(expertDao, applicationDao, ratingDao);
    }

    private Expert createExpert(String login, String... directions) {
        Expert expert = new Expert(login, "hash", "Имя", "Фамилия", Set.of(directions));
        expertDao.save(expert);
        return expert;
    }

    private Application createApplication(String owner, long amount, String... directions) {
        Application app = new Application(UUID.randomUUID(), owner, "t", "d",
                Set.of(directions), amount);
        applicationDao.save(app);
        return app;
    }

    @Test
    void rateStoresRatingForExpertAndApplication() {
        createExpert("expert1", "math");
        Application app = createApplication("ivan", 100, "math");

        expertService.rate("expert1", app.getId(), 5);

        assertEquals(5, ratingDao.findByApplication(app.getId()).get("expert1"));
        assertEquals(5, ratingDao.findByExpert("expert1").get(app.getId()));
    }

    @Test
    void rateOverwritesPreviousRating() {
        createExpert("expert1", "math");
        Application app = createApplication("ivan", 100, "math");

        expertService.rate("expert1", app.getId(), 3);
        expertService.rate("expert1", app.getId(), 5);

        assertEquals(5, ratingDao.findByApplication(app.getId()).get("expert1"));
    }

    @Test
    void rateThrowsInvalidRatingExceptionForZero() {
        createExpert("expert1", "math");
        Application app = createApplication("ivan", 100, "math");

        assertThrows(InvalidRatingException.class,
                () -> expertService.rate("expert1", app.getId(), 0));
    }

    @Test
    void rateThrowsInvalidRatingExceptionForSix() {
        createExpert("expert1", "math");
        Application app = createApplication("ivan", 100, "math");

        assertThrows(InvalidRatingException.class,
                () -> expertService.rate("expert1", app.getId(), 6));
    }

    @Test
    void rateThrowsInvalidRatingExceptionForNegative() {
        createExpert("expert1", "math");
        Application app = createApplication("ivan", 100, "math");

        assertThrows(InvalidRatingException.class,
                () -> expertService.rate("expert1", app.getId(), -1));
    }

    @Test
    void rateThrowsNotFoundForUnknownApplication() {
        createExpert("expert1", "math");

        assertThrows(NotFoundException.class,
                () -> expertService.rate("expert1", UUID.randomUUID(), 5));
    }

    @Test
    void rateDoesNotStoreRatingForUnknownApplication() {
        createExpert("expert1", "math");
        UUID unknownId = UUID.randomUUID();

        assertThrows(NotFoundException.class,
                () -> expertService.rate("expert1", unknownId, 5));

        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
    }

    @Test
    void rateStoresRatingsFromSeveralExperts() {
        createExpert("expert1", "math");
        createExpert("expert2", "math");
        Application app = createApplication("ivan", 100, "math");

        expertService.rate("expert1", app.getId(), 5);
        expertService.rate("expert2", app.getId(), 3);

        assertEquals(2, ratingDao.findByApplication(app.getId()).size());
    }

    @Test
    void removeRatingDeletesStoredRating() {
        createExpert("expert1", "math");
        Application app = createApplication("ivan", 100, "math");
        expertService.rate("expert1", app.getId(), 5);

        expertService.removeRating("expert1", app.getId());

        assertTrue(ratingDao.findByApplication(app.getId()).isEmpty());
        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
    }

    @Test
    void removeRatingKeepsOtherExpertsRatings() {
        createExpert("expert1", "math");
        createExpert("expert2", "math");
        Application app = createApplication("ivan", 100, "math");
        expertService.rate("expert1", app.getId(), 5);
        expertService.rate("expert2", app.getId(), 3);

        expertService.removeRating("expert1", app.getId());

        assertEquals(3, ratingDao.findByApplication(app.getId()).get("expert2"));
    }

    @Test
    void removeRatingIsNoOpForUnknownRating() {
        createExpert("expert1", "math");
        Application app = createApplication("ivan", 100, "math");

        assertDoesNotThrow(() -> expertService.removeRating("expert1", app.getId()));
    }

    @Test
    void getApplicationsByDirectionsReturnsMatchingApplications() {
        createExpert("expert1", "math", "physics");
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 200, "physics");
        createApplication("sergey", 300, "chemistry");

        List<Application> result = expertService.getApplicationsByDirections(
                "expert1", Set.of("math", "physics"));

        assertEquals(2, result.size());
        assertTrue(result.contains(a));
        assertTrue(result.contains(b));
    }

    @Test
    void getApplicationsByDirectionsRestrictsToExpertsDirections() {
        createExpert("expert1", "math");
        createApplication("ivan", 100, "math");
        createApplication("petr", 200, "physics");

        List<Application> result = expertService.getApplicationsByDirections(
                "expert1", Set.of("math", "physics"));

        assertEquals(1, result.size());
        assertEquals(Set.of("math"), result.get(0).getDirections());
    }

    @Test
    void getApplicationsByDirectionsReturnsEmptyWhenNoIntersection() {
        createExpert("expert1", "math");
        createApplication("ivan", 100, "physics");

        List<Application> result = expertService.getApplicationsByDirections(
                "expert1", Set.of("physics"));

        assertTrue(result.isEmpty());
    }

    @Test
    void getApplicationsByDirectionsReturnsEmptyForEmptyRequest() {
        createExpert("expert1", "math");
        createApplication("ivan", 100, "math");

        List<Application> result = expertService.getApplicationsByDirections(
                "expert1", Set.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void getApplicationsByDirectionsThrowsNotFoundForUnknownExpert() {
        assertThrows(NotFoundException.class,
                () -> expertService.getApplicationsByDirections("unknown", Set.of("math")));
    }

    @Test
    void getRatedApplicationsReturnsEmptyWhenNoRatings() {
        createExpert("expert1", "math");

        assertTrue(expertService.getRatedApplications("expert1").isEmpty());
    }

    @Test
    void getRatedApplicationsReturnsApplicationsRatedByExpert() {
        createExpert("expert1", "math");
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 200, "math");
        expertService.rate("expert1", a.getId(), 5);
        expertService.rate("expert1", b.getId(), 3);

        List<Application> result = expertService.getRatedApplications("expert1");

        assertEquals(2, result.size());
        assertTrue(result.contains(a));
        assertTrue(result.contains(b));
    }

    @Test
    void getRatedApplicationsDoesNotReturnApplicationsRatedByOthers() {
        createExpert("expert1", "math");
        createExpert("expert2", "math");
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 200, "math");
        expertService.rate("expert1", a.getId(), 5);
        expertService.rate("expert2", b.getId(), 3);

        List<Application> result = expertService.getRatedApplications("expert1");

        assertEquals(1, result.size());
        assertSame(a, result.get(0));
    }

    @Test
    void getRatedApplicationsSkipsDeletedApplications() {
        createExpert("expert1", "math");
        Application a = createApplication("ivan", 100, "math");
        expertService.rate("expert1", a.getId(), 5);

        applicationDao.delete(a.getId());

        List<Application> result = expertService.getRatedApplications("expert1");

        assertTrue(result.isEmpty());
    }


    @Test
    void deleteAllDataRemovesAllRatingsOfExpert() {
        createExpert("expert1", "math");
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 200, "math");
        expertService.rate("expert1", a.getId(), 5);
        expertService.rate("expert1", b.getId(), 3);

        expertService.deleteAllData("expert1");

        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
        assertTrue(ratingDao.findByApplication(a.getId()).isEmpty());
        assertTrue(ratingDao.findByApplication(b.getId()).isEmpty());
    }

    @Test
    void deleteAllDataKeepsRatingsOfOtherExperts() {
        createExpert("expert1", "math");
        createExpert("expert2", "math");
        Application app = createApplication("ivan", 100, "math");
        expertService.rate("expert1", app.getId(), 5);
        expertService.rate("expert2", app.getId(), 3);

        expertService.deleteAllData("expert1");

        assertEquals(3, ratingDao.findByApplication(app.getId()).get("expert2"));
        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
    }

    @Test
    void deleteAllDataIsNoOpForExpertWithoutRatings() {
        createExpert("expert1", "math");

        assertDoesNotThrow(() -> expertService.deleteAllData("expert1"));
        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
    }
}