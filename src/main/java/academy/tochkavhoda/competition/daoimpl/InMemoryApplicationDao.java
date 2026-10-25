package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.dao.ApplicationDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class InMemoryApplicationDao implements ApplicationDao {

    private final Database db;

    public InMemoryApplicationDao() {
        this.db = Database.getInstance();
    }

    @Override
    public Application save(Application application) {
        UUID id = application.getId();

        if (db.getApplicationsById().containsKey(id)) {
            throw new IllegalStateException("application already exists: " + id);
        }

        db.getApplicationsById().put(id, application);
        db.getApplicationsByOwner()
                .computeIfAbsent(application.getOwnerLogin(), k -> new HashSet<>())
                .add(id);
        for (String direction : application.getDirections()) {
            db.getApplicationsByDirection()
                    .computeIfAbsent(direction, k -> new HashSet<>())
                    .add(id);
        }
        return application;
    }

    @Override
    public Application findById(UUID id) {
        Application application = db.getApplicationsById().get(id);
        if (application == null) {
            throw new NotFoundException(id.toString());
        }
        return application;
    }

    @Override
    public List<Application> findByOwner(String ownerLogin) {
        Set<UUID> ids = db.getApplicationsByOwner().getOrDefault(ownerLogin, Set.of());
        return toApplications(ids);
    }

    @Override
    public List<Application> findByDirections(Set<String> directions) {
        Set<UUID> ids = new HashSet<>();
        for (String direction : directions) {
            ids.addAll(db.getApplicationsByDirection().getOrDefault(direction, Set.of()));
        }
        return toApplications(ids);
    }

    @Override
    public void delete(UUID id) {
        Application application = db.getApplicationsById().get(id);
        if (application == null) {
            throw new NotFoundException(id.toString());
        }

        db.getApplicationsById().remove(id);

        Set<UUID> ownerApps = db.getApplicationsByOwner().get(application.getOwnerLogin());
        if (ownerApps != null) {
            ownerApps.remove(id);
            if (ownerApps.isEmpty()) {
                db.getApplicationsByOwner().remove(application.getOwnerLogin());
            }
        }

        for (String direction : application.getDirections()) {
            Set<UUID> set = db.getApplicationsByDirection().get(direction);
            if (set != null) {
                set.remove(id);
                if (set.isEmpty()) {
                    db.getApplicationsByDirection().remove(direction);
                }
            }
        }
    }

    private List<Application> toApplications(Set<UUID> ids) {
        List<Application> result = new ArrayList<>();
        for (UUID id : ids) {
            Application application = db.getApplicationsById().get(id);
            if (application != null) {
                result.add(application);
            }
        }
        return result;
    }

    @Override
    public List<Application> findAll() {
        return new ArrayList<>(db.getApplicationsById().values());
    }
}
