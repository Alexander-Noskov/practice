package academy.tochkavhoda.competition.dao;

import academy.tochkavhoda.competition.model.Application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ApplicationDao {
    Application save(Application application);
    Application findById(UUID id);
    List<Application> findByOwner(String ownerLogin);
    List<Application> findByDirections(Set<String> directions);
    void delete(UUID id);
    List<Application> findAll();
}
