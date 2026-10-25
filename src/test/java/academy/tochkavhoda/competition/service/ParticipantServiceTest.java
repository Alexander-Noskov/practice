package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.daoimpl.InMemoryApplicationDao;
import academy.tochkavhoda.competition.daoimpl.InMemoryRatingDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.dto.request.CreateApplicationRequest;
import academy.tochkavhoda.competition.exception.AccessDeniedException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantServiceTest {
    private InMemoryApplicationDao applicationDao;
    private InMemoryRatingDao ratingDao;
    private ParticipantService participantService;

    @BeforeEach
    void setUp() {
        Database.getInstance().getParticipantsByCompany().clear();
        Database.getInstance().getParticipantsByLogin().clear();
        Database.getInstance().getApplicationsByDirection().clear();
        Database.getInstance().getApplicationsById().clear();
        Database.getInstance().getApplicationsByOwner().clear();
        Database.getInstance().getRatingsByApplication().clear();
        applicationDao = new InMemoryApplicationDao();
        ratingDao = new InMemoryRatingDao();
        participantService = new ParticipantService(applicationDao, ratingDao);
    }

    private CreateApplicationRequest request(String title, long amount, String... directions) {
        return new CreateApplicationRequest(title, "desc", Set.of(directions), amount);
    }

    @Test
    void createApplicationStoresApplicationForOwner() {
        Application created = participantService.createApplication("ivan", request("t", 100, "math"));

        assertNotNull(created.getId());
        assertEquals("ivan", created.getOwnerLogin());
        assertEquals("t", created.getTitle());
        assertEquals(100, created.getRequestedAmount());
        assertEquals(Set.of("math"), created.getDirections());

        assertSame(created, applicationDao.findById(created.getId()));
    }

    @Test
    void createApplicationAddsToOwnerIndex() {
        Application created = participantService.createApplication("ivan", request("t", 100, "math"));

        List<Application> byOwner = participantService.getMyApplications("ivan");
        assertEquals(1, byOwner.size());
        assertSame(created, byOwner.get(0));
    }

    @Test
    void createApplicationAddsToAllDirectionIndexes() {
        Application created = participantService.createApplication("ivan",
                request("t", 100, "math", "physics"));

        assertEquals(1, applicationDao.findByDirections(Set.of("math")).size());
        assertEquals(1, applicationDao.findByDirections(Set.of("physics")).size());
        assertSame(created, applicationDao.findByDirections(Set.of("math")).get(0));
    }

    @Test
    void createApplicationGeneratesDifferentIds() {
        Application a = participantService.createApplication("ivan", request("t1", 100, "math"));
        Application b = participantService.createApplication("ivan", request("t2", 200, "math"));

        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    void createApplicationThrowsForInvalidAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> participantService.createApplication("ivan",
                        new CreateApplicationRequest("t", "d", Set.of("math"), -1L)));
    }

    @Test
    void createApplicationThrowsForEmptyDirections() {
        assertThrows(IllegalArgumentException.class,
                () -> participantService.createApplication("ivan",
                        new CreateApplicationRequest("t", "d", Set.of(), 100L)));
    }

    @Test
    void cancelApplicationRemovesOwnApplication() {
        Application created = participantService.createApplication("ivan", request("t", 100, "math"));

        participantService.cancelApplication("ivan", created.getId());

        assertThrows(NotFoundException.class, () -> applicationDao.findById(created.getId()));
        assertTrue(participantService.getMyApplications("ivan").isEmpty());
        assertTrue(applicationDao.findByDirections(Set.of("math")).isEmpty());
    }

    @Test
    void cancelApplicationRemovesItsRatings() {
        Application created = participantService.createApplication("ivan", request("t", 100, "math"));
        ratingDao.save("expert1", created.getId(), 5);
        ratingDao.save("expert2", created.getId(), 3);

        participantService.cancelApplication("ivan", created.getId());

        assertTrue(ratingDao.findByApplication(created.getId()).isEmpty());
        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
        assertTrue(ratingDao.findByExpert("expert2").isEmpty());
    }

    @Test
    void cancelApplicationThrowsNotFoundForUnknownId() {
        assertThrows(NotFoundException.class,
                () -> participantService.cancelApplication("ivan", UUID.randomUUID()));
    }

    @Test
    void cancelApplicationThrowsAccessDeniedForOtherOwner() {
        Application created = participantService.createApplication("ivan", request("t", 100, "math"));

        assertThrows(AccessDeniedException.class,
                () -> participantService.cancelApplication("petr", created.getId()));

        assertSame(created, applicationDao.findById(created.getId()));
    }

    @Test
    void cancelApplicationKeepsOtherApplicationsOfOwner() {
        Application a = participantService.createApplication("ivan", request("t1", 100, "math"));
        Application b = participantService.createApplication("ivan", request("t2", 200, "physics"));

        participantService.cancelApplication("ivan", a.getId());

        List<Application> byOwner = participantService.getMyApplications("ivan");
        assertEquals(1, byOwner.size());
        assertSame(b, byOwner.get(0));
    }

    @Test
    void cancelApplicationKeepsApplicationsWithSameDirectionFromOtherOwners() {
        Application a = participantService.createApplication("ivan", request("t1", 100, "math"));
        Application b = participantService.createApplication("petr", request("t2", 200, "math"));

        participantService.cancelApplication("ivan", a.getId());

        List<Application> byMath = applicationDao.findByDirections(Set.of("math"));
        assertEquals(1, byMath.size());
        assertSame(b, byMath.get(0));
    }

    @Test
    void getMyApplicationsReturnsEmptyListForOwnerWithoutApplications() {
        assertTrue(participantService.getMyApplications("ivan").isEmpty());
    }

    @Test
    void getMyApplicationsReturnsOnlyOwnApplications() {
        Application a = participantService.createApplication("ivan", request("t1", 100, "math"));
        participantService.createApplication("petr", request("t2", 200, "math"));

        List<Application> byOwner = participantService.getMyApplications("ivan");

        assertEquals(1, byOwner.size());
        assertSame(a, byOwner.get(0));
    }

    @Test
    void getMyApplicationsReturnsAllOwnApplications() {
        Application a = participantService.createApplication("ivan", request("t1", 100, "math"));
        Application b = participantService.createApplication("ivan", request("t2", 200, "physics"));

        List<Application> byOwner = participantService.getMyApplications("ivan");

        assertEquals(2, byOwner.size());
        assertTrue(byOwner.contains(a));
        assertTrue(byOwner.contains(b));
    }


    @Test
    void deleteAllDataRemovesAllApplicationsOfOwner() {
        Application a = participantService.createApplication("ivan", request("t1", 100, "math"));
        Application b = participantService.createApplication("ivan", request("t2", 200, "physics"));

        participantService.deleteAllData("ivan");

        assertTrue(participantService.getMyApplications("ivan").isEmpty());
        assertThrows(NotFoundException.class, () -> applicationDao.findById(a.getId()));
        assertThrows(NotFoundException.class, () -> applicationDao.findById(b.getId()));
    }

    @Test
    void deleteAllDataRemovesRatingsOfDeletedApplications() {
        Application a = participantService.createApplication("ivan", request("t1", 100, "math"));
        Application b = participantService.createApplication("ivan", request("t2", 200, "physics"));
        ratingDao.save("expert1", a.getId(), 5);
        ratingDao.save("expert1", b.getId(), 3);

        participantService.deleteAllData("ivan");

        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
        assertTrue(ratingDao.findByApplication(a.getId()).isEmpty());
        assertTrue(ratingDao.findByApplication(b.getId()).isEmpty());
    }

    @Test
    void deleteAllDataKeepsApplicationsOfOtherOwners() {
        Application ivanApp = participantService.createApplication("ivan", request("t1", 100, "math"));
        Application petrApp = participantService.createApplication("petr", request("t2", 200, "math"));

        participantService.deleteAllData("ivan");

        assertTrue(participantService.getMyApplications("ivan").isEmpty());
        assertEquals(1, participantService.getMyApplications("petr").size());
        assertSame(petrApp, applicationDao.findById(petrApp.getId()));
        assertThrows(NotFoundException.class, () -> applicationDao.findById(ivanApp.getId()));
    }

    @Test
    void deleteAllDataKeepsRatingsOfOtherApplications() {
        Application ivanApp = participantService.createApplication("ivan", request("t1", 100, "math"));
        Application petrApp = participantService.createApplication("petr", request("t2", 200, "math"));
        ratingDao.save("expert1", ivanApp.getId(), 5);
        ratingDao.save("expert1", petrApp.getId(), 3);

        participantService.deleteAllData("ivan");

        assertTrue(ratingDao.findByApplication(ivanApp.getId()).isEmpty());
        assertEquals(3, ratingDao.findByApplication(petrApp.getId()).get("expert1"));
        assertEquals(1, ratingDao.findByExpert("expert1").size());
    }

    @Test
    void deleteAllDataIsNoOpForOwnerWithoutApplications() {
        assertDoesNotThrow(() -> participantService.deleteAllData("ivan"));
        assertTrue(participantService.getMyApplications("ivan").isEmpty());
    }
}