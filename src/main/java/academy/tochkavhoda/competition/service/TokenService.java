package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.TokenDao;

import java.util.UUID;

public class TokenService {
    private final TokenDao tokenDao;

    public TokenService(TokenDao tokenDao) {
        this.tokenDao = tokenDao;
    }

    public String issue(String login) {
        tokenDao.deleteByLogin(login);
        String token = generateToken();
        tokenDao.save(token, login);
        return token;
    }

    public String authenticate(String token) {
        return tokenDao.findLoginByToken(token);
    }

    public void logout(String token) {
        tokenDao.delete(token);
    }

    public void revokeByLogin(String login) {
        tokenDao.deleteByLogin(login);
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}
