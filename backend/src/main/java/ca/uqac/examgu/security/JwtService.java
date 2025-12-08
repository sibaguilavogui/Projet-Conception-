package ca.uqac.examgu.security;

import ca.uqac.examgu.model.Utilisateur;
import ca.uqac.examgu.repository.UtilisateurRepository;
import ca.uqac.examgu.service.UtilisateurDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {
    private final UtilisateurRepository repo;
    private final String ENCRIPTION_KEY = "b3f58e973e5ab076ccd78f541be173eab0087a2d6540e296385f734cd3c2ab63";


    public JwtService(UtilisateurRepository repo, UtilisateurDetailsServiceImpl utilisateurDetailsService) {
        this.repo = repo;
    }

    public Map<String, String> generate(String email) {
        Optional<Utilisateur> utilisateur = repo.findByEmail(email);
        if (utilisateur.isPresent()) {
            return this.generateJwt(utilisateur.get());
        }
        return null;

    }

    public String extractEmail(String token) {
        try {
            return this.getClaim(token, Claims::getSubject);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getAllClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            System.out.println("Token expiration: " + expiration);
            System.out.println("Current time: " + now);
            System.out.println("Is expired: " + expiration.before(now));
            return expiration.before(now);
        } catch (Exception e) {
            return true;
        }
    }


    public Date getExpirationDateFromToken(String token) {
        return this.getClaim(token, Claims::getExpiration);
    }

    private <T> T getClaim(String token, Function<Claims, T> function) {
        Claims claims = getAllClaims(token);
        return function.apply(claims);
    }

    Claims getAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(this.getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Map<String, String> generateJwt(Utilisateur utilisateur) {
        final long currentTime = System.currentTimeMillis();
        final long expirationTime = currentTime + 240 * 60 * 1000;

        final Map<String, Object> claims = new HashMap<>();
        if (utilisateur.getRole() != null) claims.put("role", utilisateur.getRole());

        final String bearer = Jwts.builder()
                .setSubject(utilisateur.getEmail())
                .claims(claims)
                .setExpiration(new Date(expirationTime))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
        return Map.of("bearer", bearer);
    }

    private Key getKey() {
        try {
            byte[] keyBytes = HexFormat.of().parseHex(ENCRIPTION_KEY);
            System.out.println("Key length: " + keyBytes.length + " bytes");
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            System.out.println("Error parsing key: " + e.getMessage());
            throw e;
        }
    }

}