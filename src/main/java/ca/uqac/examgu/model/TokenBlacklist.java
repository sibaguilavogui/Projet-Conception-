package ca.uqac.examgu.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens_blacklist")
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(name = "date_blacklist", nullable = false)
    private LocalDateTime dateBlacklist;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    public TokenBlacklist() {}

    public TokenBlacklist(String token, String email, LocalDateTime dateExpiration) {
        this.token = token;
        this.email = email;
        this.dateBlacklist = LocalDateTime.now();
        this.dateExpiration = dateExpiration;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getDateBlacklist() { return dateBlacklist; }
    public void setDateBlacklist(LocalDateTime dateBlacklist) { this.dateBlacklist = dateBlacklist; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }

    public boolean estExpire() {
        return dateExpiration != null && LocalDateTime.now().isAfter(dateExpiration);
    }
}