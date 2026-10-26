package academy.tochkavhoda.competition.server;

import academy.tochkavhoda.competition.dto.request.CancelApplicationRequest;
import academy.tochkavhoda.competition.dto.request.CreateApplicationRequest;
import academy.tochkavhoda.competition.dto.request.DirectionsRequest;
import academy.tochkavhoda.competition.dto.request.GrantRequest;
import academy.tochkavhoda.competition.dto.request.LoginRequest;
import academy.tochkavhoda.competition.dto.request.RateRequest;
import academy.tochkavhoda.competition.dto.request.RegisterExpertRequest;
import academy.tochkavhoda.competition.dto.request.RegisterParticipantRequest;
import academy.tochkavhoda.competition.dto.request.RemoveRatingRequest;
import academy.tochkavhoda.competition.dto.response.GrantResult;
import academy.tochkavhoda.competition.dto.response.TokenDto;
import academy.tochkavhoda.competition.exception.AccessDeniedException;
import academy.tochkavhoda.competition.exception.AuthException;
import academy.tochkavhoda.competition.exception.DuplicateCompanyException;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.exception.InvalidRatingException;
import academy.tochkavhoda.competition.exception.NotFoundException;
import academy.tochkavhoda.competition.model.Application;
import academy.tochkavhoda.competition.model.Expert;
import academy.tochkavhoda.competition.model.Participant;
import academy.tochkavhoda.competition.dto.response.ServerResponse;
import academy.tochkavhoda.competition.service.AuthService;
import academy.tochkavhoda.competition.service.ExpertService;
import academy.tochkavhoda.competition.service.GrantService;
import academy.tochkavhoda.competition.service.ParticipantService;
import academy.tochkavhoda.competition.util.JsonUtil;
import com.google.gson.JsonSyntaxException;

import java.util.List;

public class Server {
    private final AuthService authService;
    private final ParticipantService participantService;
    private final ExpertService expertService;
    private final GrantService grantService;

    public Server(AuthService authService,
                  ParticipantService participantService,
                  ExpertService expertService,
                  GrantService grantService) {
        this.authService = authService;
        this.participantService = participantService;
        this.expertService = expertService;
        this.grantService = grantService;
    }

    public ServerResponse registerParticipant(String json) {
        return execute(() -> {
            RegisterParticipantRequest request =
                    JsonUtil.fromJson(json, RegisterParticipantRequest.class);
            String token = authService.registerParticipant(request);
            return JsonUtil.ok(new TokenDto(token));
        });
    }

    public ServerResponse registerExpert(String json) {
        return execute(() -> {
            RegisterExpertRequest request =
                    JsonUtil.fromJson(json, RegisterExpertRequest.class);
            String token = authService.registerExpert(request);
            return JsonUtil.ok(new TokenDto(token));
        });
    }

    public ServerResponse login(String json) {
        return execute(() -> {
            LoginRequest request = JsonUtil.fromJson(json, LoginRequest.class);
            String token = authService.login(request);
            return JsonUtil.ok(new TokenDto(token));
        });
    }

    public ServerResponse logout(String token) {
        return execute(() -> {
            authService.logout(token);
            return JsonUtil.okEmpty();
        });
    }

    public ServerResponse leave(String token) {
        return execute(() -> {
            authService.leave(token);
            return JsonUtil.okEmpty();
        });
    }

    public ServerResponse createApplication(String token, String json) {
        return execute(() -> {
            Participant participant = authService.authenticateParticipant(token);
            CreateApplicationRequest request =
                    JsonUtil.fromJson(json, CreateApplicationRequest.class);
            Application app = participantService.createApplication(participant.getLogin(), request);
            return JsonUtil.ok(app);
        });
    }

    public ServerResponse cancelApplication(String token, String json) {
        return execute(() -> {
            Participant participant = authService.authenticateParticipant(token);
            CancelApplicationRequest request =
                    JsonUtil.fromJson(json, CancelApplicationRequest.class);
            participantService.cancelApplication(participant.getLogin(), request.applicationId());
            return JsonUtil.okEmpty();
        });
    }

    public ServerResponse getMyApplications(String token) {
        return execute(() -> {
            Participant participant = authService.authenticateParticipant(token);
            List<Application> apps = participantService.getMyApplications(participant.getLogin());
            return JsonUtil.ok(apps);
        });
    }

    public ServerResponse rate(String token, String json) {
        return execute(() -> {
            Expert expert = authService.authenticateExpert(token);
            RateRequest request = JsonUtil.fromJson(json, RateRequest.class);
            expertService.rate(expert.getLogin(), request.applicationId(), request.value());
            return JsonUtil.okEmpty();
        });
    }

    public ServerResponse removeRating(String token, String json) {
        return execute(() -> {
            Expert expert = authService.authenticateExpert(token);
            RemoveRatingRequest request = JsonUtil.fromJson(json, RemoveRatingRequest.class);
            expertService.removeRating(expert.getLogin(), request.applicationId());
            return JsonUtil.okEmpty();
        });
    }

    public ServerResponse getApplicationsByDirections(String token, String json) {
        return execute(() -> {
            Expert expert = authService.authenticateExpert(token);
            DirectionsRequest request = JsonUtil.fromJson(json, DirectionsRequest.class);
            List<Application> apps = expertService.getApplicationsByDirections(
                    expert.getLogin(), request.directions());
            return JsonUtil.ok(apps);
        });
    }

    public ServerResponse getRatedApplications(String token) {
        return execute(() -> {
            Expert expert = authService.authenticateExpert(token);
            List<Application> apps = expertService.getRatedApplications(expert.getLogin());
            return JsonUtil.ok(apps);
        });
    }

    public ServerResponse distributeGrants(String json) {
        return execute(() -> {
            GrantRequest request = JsonUtil.fromJson(json, GrantRequest.class);
            GrantResult result = grantService.distribute(
                    request.totalFund(), request.threshold());
            return JsonUtil.ok(result);
        });
    }

    private ServerResponse execute(ServerAction action) {
        try {
            return action.run();
        } catch (JsonSyntaxException | NullPointerException | IllegalArgumentException | NotFoundException |
                 DuplicateLoginException | DuplicateCompanyException | InvalidRatingException e) {
            return JsonUtil.error(400, e.getMessage());
        } catch (AuthException e) {
            return JsonUtil.error(401, e.getMessage());
        } catch (AccessDeniedException e) {
            return JsonUtil.error(403, e.getMessage());
        } catch (RuntimeException e) {
            return JsonUtil.error(500, "internal error");
        }
    }

    @FunctionalInterface
    private interface ServerAction {
        ServerResponse run();
    }
}
