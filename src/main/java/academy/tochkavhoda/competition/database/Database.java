package academy.tochkavhoda.competition.database;

import academy.tochkavhoda.competition.model.Application;
import academy.tochkavhoda.competition.model.Expert;
import academy.tochkavhoda.competition.model.Participant;
import academy.tochkavhoda.competition.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Database {
    private final Map<String, User> usersByLogin = new HashMap<>();
    private final Map<String, Participant> participantsByLogin = new HashMap<>();
    private final Map<String, Participant> participantsByCompany = new HashMap<>();
    private final Map<String, Expert> expertsByLogin = new HashMap<>();

    private final Map<String, String> tokens = new HashMap<>();
    private final Map<String, String> tokenByLogin = new HashMap<>();

    private final Map<String, Set<UUID>> applicationsByOwner = new HashMap<>();
    private final Map<UUID, Application> applicationsById = new HashMap<>();
    private final Map<String, Set<UUID>> applicationsByDirection = new HashMap<>();

    private final Map<UUID, Map<String, Integer>> ratingsByApplication = new HashMap<>();

    private static final Database INSTANCE = new Database();

    private Database() {
    }

    public static Database getInstance() {
        return INSTANCE;
    }

    public Map<String, User> getUsersByLogin() {
        return usersByLogin;
    }

    public Map<String, Participant> getParticipantsByLogin() {
        return participantsByLogin;
    }

    public Map<String, Participant> getParticipantsByCompany() {
        return participantsByCompany;
    }

    public Map<String, Set<UUID>> getApplicationsByOwner() {
        return applicationsByOwner;
    }

    public Map<UUID, Application> getApplicationsById() {
        return applicationsById;
    }

    public Map<String, Set<UUID>> getApplicationsByDirection() {
        return applicationsByDirection;
    }

    public Map<UUID, Map<String, Integer>> getRatingsByApplication() {
        return ratingsByApplication;
    }

    public Map<String, Expert> getExpertsByLogin() {
        return expertsByLogin;
    }

    public Map<String, String> getTokens() {
        return tokens;
    }

    public Map<String, String> getTokenByLogin() {
        return tokenByLogin;
    }
}
