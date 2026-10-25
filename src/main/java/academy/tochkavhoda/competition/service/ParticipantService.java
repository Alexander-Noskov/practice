package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.ApplicationDao;
import academy.tochkavhoda.competition.dao.RatingDao;
import academy.tochkavhoda.competition.dto.request.CreateApplicationRequest;
import academy.tochkavhoda.competition.exception.AccessDeniedException;
import academy.tochkavhoda.competition.model.Application;

import java.util.List;
import java.util.UUID;

public class ParticipantService {
    private final ApplicationDao applicationDao;
    private final RatingDao ratingDao;

    public ParticipantService(ApplicationDao applicationDao, RatingDao ratingDao) {
        this.applicationDao = applicationDao;
        this.ratingDao = ratingDao;
    }

    public Application createApplication(String ownerLogin, CreateApplicationRequest request) {
        Application application = new Application(
                UUID.randomUUID(),
                ownerLogin,
                request.title(),
                request.description(),
                request.directions(),
                request.requestedAmount()
        );

        return applicationDao.save(application);
    }

    public void cancelApplication(String ownerLogin, UUID applicationId) {
        Application application = applicationDao.findById(applicationId);

        if (!application.getOwnerLogin().equals(ownerLogin)) {
            throw new AccessDeniedException("not the owner of the application");
        }

        ratingDao.deleteAllByApplication(applicationId);
        applicationDao.delete(applicationId);
    }

    public List<Application> getMyApplications(String ownerLogin) {
        return applicationDao.findByOwner(ownerLogin);
    }


    public void deleteAllData(String ownerLogin) {
        List<Application> applications = applicationDao.findByOwner(ownerLogin);
        for (Application application : applications) {
            ratingDao.deleteAllByApplication(application.getId());
            applicationDao.delete(application.getId());
        }
    }
}
