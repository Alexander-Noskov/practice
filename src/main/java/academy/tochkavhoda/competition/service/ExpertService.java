package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.ApplicationDao;
import academy.tochkavhoda.competition.dao.ExpertDao;
import academy.tochkavhoda.competition.dao.RatingDao;
import academy.tochkavhoda.competition.exception.InvalidRatingException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Application;
import academy.tochkavhoda.competition.model.Expert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ExpertService {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final ExpertDao expertDao;
    private final ApplicationDao applicationDao;
    private final RatingDao ratingDao;

    public ExpertService(ExpertDao expertDao,
                         ApplicationDao applicationDao,
                         RatingDao ratingDao) {
        this.expertDao = expertDao;
        this.applicationDao = applicationDao;
        this.ratingDao = ratingDao;
    }

    public void rate(String expertLogin, UUID applicationId, int value) {
        if (value < MIN_RATING || value > MAX_RATING) {
            throw new InvalidRatingException(Integer.toString(value));
        }

        applicationDao.findById(applicationId);
        ratingDao.save(expertLogin, applicationId, value);
    }

    public void removeRating(String expertLogin, UUID applicationId) {
        ratingDao.delete(expertLogin, applicationId);
    }

    public List<Application> getApplicationsByDirections(String expertLogin, Set<String> requestedDirections) {
        Expert expert = expertDao.findByLogin(expertLogin);

        Set<String> allowed = new HashSet<>(requestedDirections);
        allowed.retainAll(expert.getDirections());

        if (allowed.isEmpty()) {
            return List.of();
        }
        return applicationDao.findByDirections(allowed);
    }

    public List<Application> getRatedApplications(String expertLogin) {
        Map<UUID, Integer> ratings = ratingDao.findByExpert(expertLogin);

        List<Application> result = new ArrayList<>();
        for (UUID applicationId : ratings.keySet()) {
            try {
                result.add(applicationDao.findById(applicationId));
            } catch (NotFoundException ignored) {
            }
        }
        return result;
    }


    public void deleteAllData(String expertLogin) {
        ratingDao.deleteAllByExpert(expertLogin);
    }
}
