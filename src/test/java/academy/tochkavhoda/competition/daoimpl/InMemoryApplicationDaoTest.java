package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryApplicationDaoTest {
    private InMemoryApplicationDao applicationDao;

    @BeforeEach
    void setUp() {
        Database.getInstance().getApplicationsById().clear();
        Database.getInstance().getApplicationsByOwner().clear();
        Database.getInstance().getApplicationsByDirection().clear();
        applicationDao = new InMemoryApplicationDao();
    }

    private Application createApplication(String ownerLogin, long amount, String... directions) {
        return new Application(UUID.randomUUID(), ownerLogin, "title", "desc",
                Set.of(directions), amount);
    }

    @Test
    void saveReturnsSameApplicationAndStoresIt() {
        Application app = createApplication("ivan", 100, "math");

        Application saved = applicationDao.save(app);

        assertSame(app, saved);
        assertSame(app, applicationDao.findById(app.getId()));
    }

    @Test
    void saveAddsApplicationToOwnerIndex() {
        Application app = createApplication("ivan", 100, "math");

        applicationDao.save(app);

        List<Application> byOwner = applicationDao.findByOwner("ivan");
        assertEquals(1, byOwner.size());
        assertSame(app, byOwner.get(0));
    }

    @Test
    void saveAddsApplicationToAllDirectionIndexes() {
        Application app = createApplication("ivan", 100, "math", "physics");

        applicationDao.save(app);

        assertEquals(1, applicationDao.findByDirections(Set.of("math")).size());
        assertEquals(1, applicationDao.findByDirections(Set.of("physics")).size());
        assertSame(app, applicationDao.findByDirections(Set.of("math")).get(0));
    }

    @Test
    void saveThrowsWhenApplicationWithSameIdExists() {
        Application app = createApplication("ivan", 100, "math");
        applicationDao.save(app);

        Application duplicate = new Application(app.getId(), "petr", "t", "d",
                Set.of("math"), 200L);

        assertThrows(IllegalStateException.class, () -> applicationDao.save(duplicate));
    }

    @Test
    void saveStoresApplicationsForDifferentOwnersIndependently() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 200, "math");

        applicationDao.save(a);
        applicationDao.save(b);

        assertEquals(1, applicationDao.findByOwner("ivan").size());
        assertEquals(1, applicationDao.findByOwner("petr").size());
        assertEquals(2, applicationDao.findByDirections(Set.of("math")).size());
    }

    @Test
    void findByIdReturnsStoredApplication() {
        Application app = createApplication("ivan", 100, "math");
        applicationDao.save(app);

        assertSame(app, applicationDao.findById(app.getId()));
    }

    @Test
    void findByIdThrowsNotFoundExceptionForUnknownId() {
        assertThrows(NotFoundException.class,
                () -> applicationDao.findById(UUID.randomUUID()));
    }

    @Test
    void findByOwnerReturnsEmptyListWhenOwnerHasNoApplications() {
        assertTrue(applicationDao.findByOwner("ivan").isEmpty());
    }

    @Test
    void findByOwnerReturnsAllApplicationsOfOwner() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("ivan", 200, "physics");
        applicationDao.save(a);
        applicationDao.save(b);

        List<Application> byOwner = applicationDao.findByOwner("ivan");

        assertEquals(2, byOwner.size());
        assertTrue(byOwner.contains(a));
        assertTrue(byOwner.contains(b));
    }

    @Test
    void findByOwnerDoesNotReturnApplicationsOfOtherOwners() {
        applicationDao.save(createApplication("ivan", 100, "math"));
        applicationDao.save(createApplication("petr", 200, "math"));

        List<Application> byOwner = applicationDao.findByOwner("ivan");

        assertEquals(1, byOwner.size());
        assertEquals("ivan", byOwner.get(0).getOwnerLogin());
    }

    @Test
    void findByDirectionsReturnsEmptyListWhenNoMatches() {
        assertTrue(applicationDao.findByDirections(Set.of("math")).isEmpty());
    }

    @Test
    void findByDirectionsReturnsApplicationsMatchingAnyDirection() {
        Application a = createApplication("ivan", 100, "math", "physics");
        Application b = createApplication("petr", 200, "chemistry");
        applicationDao.save(a);
        applicationDao.save(b);

        List<Application> byMath = applicationDao.findByDirections(Set.of("math"));
        List<Application> byChem = applicationDao.findByDirections(Set.of("chemistry"));

        assertEquals(1, byMath.size());
        assertSame(a, byMath.get(0));
        assertEquals(1, byChem.size());
        assertSame(b, byChem.get(0));
    }

    @Test
    void findByDirectionsCombinesResultsWithoutDuplicates() {
        Application app = createApplication("ivan", 100, "math", "physics");
        applicationDao.save(app);

        List<Application> result = applicationDao.findByDirections(Set.of("math", "physics"));

        assertEquals(1, result.size());
        assertSame(app, result.get(0));
    }

    @Test
    void findByDirectionsReturnsApplicationsFromDifferentOwners() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 200, "math");
        applicationDao.save(a);
        applicationDao.save(b);

        List<Application> result = applicationDao.findByDirections(Set.of("math"));

        assertEquals(2, result.size());
    }

    @Test
    void deleteRemovesApplicationFromAllIndexes() {
        Application app = createApplication("ivan", 100, "math", "physics");
        applicationDao.save(app);

        applicationDao.delete(app.getId());

        assertThrows(NotFoundException.class, () -> applicationDao.findById(app.getId()));
        assertTrue(applicationDao.findByOwner("ivan").isEmpty());
        assertTrue(applicationDao.findByDirections(Set.of("math")).isEmpty());
        assertTrue(applicationDao.findByDirections(Set.of("physics")).isEmpty());
    }

    @Test
    void deleteThrowsNotFoundExceptionForUnknownId() {
        assertThrows(NotFoundException.class,
                () -> applicationDao.delete(UUID.randomUUID()));
    }

    @Test
    void deleteKeepsOtherApplicationsOfSameOwner() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("ivan", 200, "physics");
        applicationDao.save(a);
        applicationDao.save(b);

        applicationDao.delete(a.getId());

        List<Application> byOwner = applicationDao.findByOwner("ivan");
        assertEquals(1, byOwner.size());
        assertSame(b, byOwner.get(0));
    }

    @Test
    void deleteKeepsApplicationsWithSameDirectionFromOtherOwners() {
        Application a = createApplication("ivan", 100, "math");
        Application b = createApplication("petr", 200, "math");
        applicationDao.save(a);
        applicationDao.save(b);

        applicationDao.delete(a.getId());

        List<Application> byMath = applicationDao.findByDirections(Set.of("math"));
        assertEquals(1, byMath.size());
        assertSame(b, byMath.get(0));
    }

    @Test
    void deleteAllowsSavingApplicationWithSameIdAgain() {
        Application app = createApplication("ivan", 100, "math");
        applicationDao.save(app);
        applicationDao.delete(app.getId());

        Application again = new Application(app.getId(), "ivan", "t2", "d2",
                Set.of("math"), 300L);

        assertDoesNotThrow(() -> applicationDao.save(again));
        assertSame(again, applicationDao.findById(app.getId()));
    }

}