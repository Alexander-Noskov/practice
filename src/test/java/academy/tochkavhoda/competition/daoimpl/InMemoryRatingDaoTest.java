package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.database.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRatingDaoTest {
    private InMemoryRatingDao ratingDao;
    private UUID applicationId;
    private UUID otherApplicationId;

    @BeforeEach
    void setUp() {
        Database.getInstance().getRatingsByApplication().clear();
        ratingDao = new InMemoryRatingDao();
        applicationId = UUID.randomUUID();
        otherApplicationId = UUID.randomUUID();
    }

    @Test
    void saveStoresRatingForExpertAndApplication() {
        ratingDao.save("expert1", applicationId, 5);

        assertEquals(Map.of("expert1", 5), ratingDao.findByApplication(applicationId));
        assertEquals(Map.of(applicationId, 5), ratingDao.findByExpert("expert1"));
    }

    @Test
    void saveOverwritesExistingRating() {
        ratingDao.save("expert1", applicationId, 3);

        ratingDao.save("expert1", applicationId, 5);

        assertEquals(Map.of("expert1", 5), ratingDao.findByApplication(applicationId));
    }

    @Test
    void saveStoresRatingsFromSeveralExpertsForSameApplication() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert2", applicationId, 3);

        Map<String, Integer> ratings = ratingDao.findByApplication(applicationId);

        assertEquals(2, ratings.size());
        assertEquals(5, ratings.get("expert1"));
        assertEquals(3, ratings.get("expert2"));
    }

    @Test
    void saveStoresRatingsFromSameExpertForDifferentApplications() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert1", otherApplicationId, 2);

        assertEquals(Map.of(applicationId, 5, otherApplicationId, 2),
                ratingDao.findByExpert("expert1"));
    }

    @Test
    void deleteRemovesExpertRatingFromApplication() {
        ratingDao.save("expert1", applicationId, 5);

        ratingDao.delete("expert1", applicationId);

        assertTrue(ratingDao.findByApplication(applicationId).isEmpty());
        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
    }

    @Test
    void deleteKeepsOtherExpertsRatingsForSameApplication() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert2", applicationId, 3);

        ratingDao.delete("expert1", applicationId);

        assertEquals(Map.of("expert2", 3), ratingDao.findByApplication(applicationId));
    }

    @Test
    void deleteDoesNothingWhenApplicationHasNoRatings() {
        assertDoesNotThrow(() -> ratingDao.delete("expert1", applicationId));
    }

    @Test
    void deleteDoesNothingWhenExpertHasNoRatingForApplication() {
        ratingDao.save("expert2", applicationId, 4);

        assertDoesNotThrow(() -> ratingDao.delete("expert1", applicationId));

        assertEquals(Map.of("expert2", 4), ratingDao.findByApplication(applicationId));
    }

    @Test
    void findByApplicationReturnsEmptyMapWhenNoRatings() {
        assertTrue(ratingDao.findByApplication(applicationId).isEmpty());
    }

    @Test
    void findByApplicationReturnsImmutableCopy() {
        ratingDao.save("expert1", applicationId, 5);

        Map<String, Integer> ratings = ratingDao.findByApplication(applicationId);

        assertThrows(UnsupportedOperationException.class,
                () -> ratings.put("expert2", 3));
    }

    @Test
    void findByExpertReturnsEmptyMapWhenNoRatings() {
        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
    }

    @Test
    void findByExpertReturnsOnlyThisExpertsRatings() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert2", applicationId, 3);

        assertEquals(Map.of(applicationId, 5), ratingDao.findByExpert("expert1"));
    }

    @Test
    void averageReturnsEmptyWhenNoRatings() {
        assertTrue(ratingDao.average(applicationId).isEmpty());
    }

    @Test
    void averageComputesMeanOfAllRatings() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert2", applicationId, 3);

        OptionalDouble average = ratingDao.average(applicationId);

        assertTrue(average.isPresent());
        assertEquals(4.0, average.getAsDouble(), 1e-9);
    }

    @Test
    void averageEqualsSingleRatingWhenOnlyOne() {
        ratingDao.save("expert1", applicationId, 5);

        assertEquals(5.0, ratingDao.average(applicationId).orElseThrow(), 1e-9);
    }

    @Test
    void deleteAllByApplicationRemovesAllItsRatings() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert2", applicationId, 3);

        ratingDao.deleteAllByApplication(applicationId);

        assertTrue(ratingDao.findByApplication(applicationId).isEmpty());
        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
        assertTrue(ratingDao.findByExpert("expert2").isEmpty());
    }

    @Test
    void deleteAllByApplicationKeepsRatingsOfOtherApplications() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert1", otherApplicationId, 2);

        ratingDao.deleteAllByApplication(applicationId);

        assertEquals(Map.of(otherApplicationId, 2), ratingDao.findByExpert("expert1"));
    }

    @Test
    void deleteAllByApplicationDoesNothingWhenNoRatings() {
        assertDoesNotThrow(() -> ratingDao.deleteAllByApplication(applicationId));
    }

    @Test
    void deleteAllByExpertRemovesAllRatingsFromExpert() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert1", otherApplicationId, 2);

        ratingDao.deleteAllByExpert("expert1");

        assertTrue(ratingDao.findByExpert("expert1").isEmpty());
    }

    @Test
    void deleteAllByExpertKeepsOtherExpertsRatings() {
        ratingDao.save("expert1", applicationId, 5);
        ratingDao.save("expert2", applicationId, 3);

        ratingDao.deleteAllByExpert("expert1");

        assertEquals(Map.of("expert2", 3), ratingDao.findByApplication(applicationId));
    }

    @Test
    void deleteAllByExpertRemovesEmptyApplicationEntries() {
        ratingDao.save("expert1", applicationId, 5);

        ratingDao.deleteAllByExpert("expert1");

        assertTrue(ratingDao.findByApplication(applicationId).isEmpty());

        assertTrue(ratingDao.average(applicationId).isEmpty());
    }

    @Test
    void deleteAllByExpertDoesNothingWhenExpertHasNoRatings() {
        ratingDao.save("expert2", applicationId, 4);

        assertDoesNotThrow(() -> ratingDao.deleteAllByExpert("expert1"));

        assertEquals(Map.of("expert2", 4), ratingDao.findByApplication(applicationId));
    }
}