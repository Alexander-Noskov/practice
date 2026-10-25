package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.dao.RatingDao;
import academy.tochkavhoda.competition.database.Database;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

public class InMemoryRatingDao implements RatingDao {

    private final Database db;

    public InMemoryRatingDao() {
        this.db = Database.getInstance();
    }

    @Override
    public void save(String expertLogin, UUID applicationId, int value) {
        db.getRatingsByApplication()
                .computeIfAbsent(applicationId, k -> new HashMap<>())
                .put(expertLogin, value);
    }

    @Override
    public void delete(String expertLogin, UUID applicationId) {
        Map<String, Integer> ratings = db.getRatingsByApplication().get(applicationId);
        if (ratings == null) {
            return;
        }
        ratings.remove(expertLogin);
        if (ratings.isEmpty()) {
            db.getRatingsByApplication().remove(applicationId);
        }
    }

    @Override
    public Map<String, Integer> findByApplication(UUID applicationId) {
        return Map.copyOf(db.getRatingsByApplication().getOrDefault(applicationId, Map.of()));
    }

    @Override
    public Map<UUID, Integer> findByExpert(String expertLogin) {
        Map<UUID, Integer> result = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Integer>> entry : db.getRatingsByApplication().entrySet()) {
            Integer value = entry.getValue().get(expertLogin);
            if (value != null) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    @Override
    public OptionalDouble average(UUID applicationId) {
        return db.getRatingsByApplication()
                .getOrDefault(applicationId, Map.of())
                .values().stream()
                .mapToInt(Integer::intValue)
                .average();
    }

    @Override
    public void deleteAllByApplication(UUID applicationId) {
        db.getRatingsByApplication().remove(applicationId);
    }

    @Override
    public void deleteAllByExpert(String expertLogin) {
        Iterator<Map.Entry<UUID, Map<String, Integer>>> iterator =
                db.getRatingsByApplication().entrySet().iterator();
        while (iterator.hasNext()) {
            Map<String, Integer> ratings = iterator.next().getValue();
            ratings.remove(expertLogin);
            if (ratings.isEmpty()) {
                iterator.remove();
            }
        }
    }
}