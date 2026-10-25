package academy.tochkavhoda.competition.dao;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

public interface RatingDao {
    void save(String expertLogin, UUID applicationId, int value);
    void delete(String expertLogin, UUID applicationId);
    Map<String, Integer> findByApplication(UUID applicationId);
    Map<UUID, Integer> findByExpert(String expertLogin);
    OptionalDouble average(UUID applicationId);
    void deleteAllByApplication(UUID applicationId);
    void deleteAllByExpert(String expertLogin);
}
