package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.ApplicationDao;
import academy.tochkavhoda.competition.dao.RatingDao;
import academy.tochkavhoda.competition.dto.response.GrantResult;
import academy.tochkavhoda.competition.model.Application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GrantService {

    private final ApplicationDao applicationDao;
    private final RatingDao ratingDao;

    public GrantService(ApplicationDao applicationDao, RatingDao ratingDao) {
        this.applicationDao = applicationDao;
        this.ratingDao = ratingDao;
    }

    public List<Application> rankApplications() {
        List<Application> rated = new ArrayList<>();
        for (Application app : applicationDao.findAll()) {
            if (ratingDao.average(app.getId()).isPresent()) {
                rated.add(app);
            }
        }

        rated.sort(Comparator
                .comparingDouble((Application a) -> ratingDao.average(a.getId()).orElseThrow())
                .reversed()
                .thenComparingLong(Application::getRequestedAmount));

        return rated;
    }

    public GrantResult distribute(long totalFund, double threshold) {
        if (totalFund < 0) {
            throw new IllegalArgumentException("totalFund must be >= 0");
        }

        List<Application> ranked = rankApplications();

        List<Application> funded = new ArrayList<>();
        List<Application> skippedByFund = new ArrayList<>();
        List<Application> skippedByThreshold = new ArrayList<>();
        long remaining = totalFund;
        boolean thresholdReached = false;

        for (Application app : ranked) {
            double avg = ratingDao.average(app.getId()).orElseThrow();

            if (thresholdReached || avg <= threshold) {
                thresholdReached = true;
                skippedByThreshold.add(app);
                continue;
            }

            long amount = app.getRequestedAmount();
            if (amount <= remaining) {
                funded.add(app);
                remaining -= amount;
            } else {
                skippedByFund.add(app);
            }
        }

        return new GrantResult(funded, skippedByFund, skippedByThreshold, remaining);
    }
}
