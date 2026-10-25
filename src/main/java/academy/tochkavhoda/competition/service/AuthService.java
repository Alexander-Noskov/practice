package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.ExpertDao;
import academy.tochkavhoda.competition.dao.ParticipantDao;
import academy.tochkavhoda.competition.dao.UserDao;
import academy.tochkavhoda.competition.dto.request.LoginRequest;
import academy.tochkavhoda.competition.dto.request.RegisterExpertRequest;
import academy.tochkavhoda.competition.dto.request.RegisterParticipantRequest;
import academy.tochkavhoda.competition.exception.AccessDeniedException;
import academy.tochkavhoda.competition.exception.AuthException;
import academy.tochkavhoda.competition.exception.DuplicateCompanyException;
import academy.tochkavhoda.competition.exception.DuplicateLoginException;
import academy.tochkavhoda.competition.model.Expert;
import academy.tochkavhoda.competition.model.Participant;
import academy.tochkavhoda.competition.model.User;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AuthService {
    private final UserDao userDao;
    private final ParticipantDao participantDao;
    private final ExpertDao expertDao;
    private final TokenService tokenService;
    private final ParticipantService participantService;
    private final ExpertService expertService;

    public AuthService(UserDao userDao, ParticipantDao participantDao, ExpertDao expertDao, TokenService tokenService,
                       ParticipantService participantService, ExpertService expertService) {
        this.userDao = userDao;
        this.participantDao = participantDao;
        this.expertDao = expertDao;
        this.tokenService = tokenService;
        this.participantService = participantService;
        this.expertService = expertService;
    }

    public String registerParticipant(RegisterParticipantRequest request) {
        if (userDao.existsByLogin(request.login())) {
            throw new DuplicateLoginException(request.login());
        }
        if (participantDao.existsByCompanyName(request.companyName())) {
            throw new DuplicateCompanyException(request.companyName());
        }

        Participant participant = new Participant(
                request.login(),
                hashPassword(request.password()),
                request.firstName(),
                request.lastName(),
                request.companyName()
        );

        userDao.save(participant);
        participantDao.save(participant);

        return tokenService.issue(participant.getLogin());
    }

    public String registerExpert(RegisterExpertRequest request) {
        Expert expert = new Expert(
                request.login(),
                hashPassword(request.password()),
                request.firstName(),
                request.lastName(),
                request.directions()
        );

        userDao.save(expert);
        expertDao.save(expert);

        return tokenService.issue(expert.getLogin());
    }

    public String login(LoginRequest request) {
        User user = userDao.findByLogin(request.login());

        if (!user.getPasswordHash().equals(hashPassword(request.password()))) {
            throw new AuthException("invalid login or password");
        }

        return tokenService.issue(user.getLogin());
    }

    public String logout(String token) {
        tokenService.authenticate(token);
        tokenService.logout(token);
        return "";
    }

    public void leave(String token) {
        User user = authenticate(token);
        String login = user.getLogin();

        tokenService.revokeByLogin(login);

        if (user instanceof Participant) {
            participantService.deleteAllData(login);
            participantDao.delete(login);
        } else if (user instanceof Expert) {
            expertService.deleteAllData(login);
            expertDao.delete(login);
        }

        userDao.delete(login);
    }

    public User authenticate(String token) {
        String login = tokenService.authenticate(token);
        return userDao.findByLogin(login);
    }

    public Participant authenticateParticipant(String token) {
        User user = authenticate(token);
        if (!(user instanceof Participant participant)) {
            throw new AccessDeniedException("participant only");
        }
        return participant;
    }

    public Expert authenticateExpert(String token) {
        User user = authenticate(token);
        if (!(user instanceof Expert expert)) {
            throw new AccessDeniedException("expert only");
        }
        return expert;
    }

    private static String hashPassword(String password) {
        return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
    }
}
