package ca.uqac.examgu.service;

import ca.uqac.examgu.model.TokenBlacklist;
import ca.uqac.examgu.repository.TokenBlacklistRepository;
import ca.uqac.examgu.security.JwtService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;

    private final Set<String> memoryCache = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TokenBlacklistService(TokenBlacklistRepository tokenBlacklistRepository, JwtService jwtService) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.jwtService = jwtService;
        initialiserCache();
    }

    @PostConstruct
    public void initialiserCache() {
        List<TokenBlacklist> tokensNonExpires = tokenBlacklistRepository.findAll().stream()
                .filter(token -> !token.estExpire())
                .collect(Collectors.toList());

        memoryCache.clear();
        tokensNonExpires.forEach(token -> memoryCache.add(token.getToken()));

        System.out.println("Cache blacklist initialisé avec " + memoryCache.size() + " tokens");
    }

    public void blacklistToken(String token) {
        if (memoryCache.contains(token)) {
            return;
        }

        String email = jwtService.extractEmail(token);
        Date expirationDate = jwtService.getExpirationDateFromToken(token);

        LocalDateTime expiration = expirationDate != null ?
                LocalDateTime.ofInstant(expirationDate.toInstant(), java.time.ZoneId.systemDefault()) :
                LocalDateTime.now().plusHours(24);

        TokenBlacklist blacklistedToken = new TokenBlacklist(token, email, expiration);
        tokenBlacklistRepository.save(blacklistedToken);

        memoryCache.add(token);

        System.out.println("Token blacklisté pour: " + email);
    }

    public boolean isTokenBlacklisted(String token) {
        if (memoryCache.contains(token)) {
            return true;
        }

        Optional<TokenBlacklist> blacklistedToken = tokenBlacklistRepository.findByToken(token);

        if (blacklistedToken.isPresent()) {
            TokenBlacklist tokenEntity = blacklistedToken.get();

            if (tokenEntity.estExpire()) {
                tokenBlacklistRepository.delete(tokenEntity);
                memoryCache.remove(token);
                return false;
            }
            memoryCache.add(token);
            return true;
        }

        return false;
    }

    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    public void nettoyerTokensExpires() {
        LocalDateTime maintenant = LocalDateTime.now();

        tokenBlacklistRepository.supprimerTokensExpires(maintenant);

        List<TokenBlacklist> tousTokens = tokenBlacklistRepository.findAll();
        Set<String> tokensValides = tousTokens.stream()
                .filter(token -> !token.estExpire())
                .map(TokenBlacklist::getToken)
                .collect(Collectors.toSet());

        memoryCache.clear();
        memoryCache.addAll(tokensValides);

        System.out.println("Nettoyage blacklist effectué - " + memoryCache.size() + " tokens valides");
    }
}