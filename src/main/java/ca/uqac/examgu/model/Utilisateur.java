package ca.uqac.examgu.model;

import jakarta.persistence.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // IDs
    private String code;          // ETU-0001 / ENS-0001 / ADM-0001 (généré par ExamGuSystem)
    private String codePermanent; // ex: DIAI01100203 (généré par ExamGuSystem)

    // Identité
    private String prenom;
    private String nom;
    private String departement;
    private LocalDate dateNaissance;

    // Auth
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String motDePasseHash; // SHA-256 hex
    private Role role;

    // ===================== Constructeurs (complet) =====================
    public Utilisateur(UUID id,
                       String email,
                       String motDePasseHash,
                       Role role,
                       String prenom,
                       String nom,
                       String departement,
                       LocalDate dateNaissance) {

        this.id = (id != null) ? id : UUID.randomUUID();
        this.email = Objects.requireNonNull(email, "email ne doit pas être null");
        this.motDePasseHash = motDePasseHash;
        this.role = Objects.requireNonNull(role, "role ne doit pas être null");

        this.prenom = normaliserTexte(prenom);
        this.nom = normaliserTexte(nom);
        this.departement = normaliserTexte(departement);
        this.dateNaissance = dateNaissance;

        this.code = null;          // généré par ExamGuSystem
        this.codePermanent = null; // généré par ExamGuSystem
    }

    public Utilisateur(String email,
                       String motDePasseEnClair,
                       Role role,
                       String prenom,
                       String nom,
                       String departement,
                       LocalDate dateNaissance) {

        this(UUID.randomUUID(), email, hashMotDePasse(motDePasseEnClair), role, prenom, nom, departement, dateNaissance);
    }

    // ===================== Anciens constructeurs (compatibilité) =====================
    // (Comme ça, ton code actuel compile encore.)
    public Utilisateur(UUID id, String email, String motDePasseHash, Role role) {
        this(id, email, motDePasseHash, role, null, null, null, null);
    }

    public Utilisateur(String email, String motDePasseEnClair, Role role) {
        this(UUID.randomUUID(), email, hashMotDePasse(motDePasseEnClair), role, null, null, null, null);
    }

    // ===================== Auth =====================
    public boolean verifierMotDePasse(String motEnClair) {
        if (motEnClair == null || motDePasseHash == null) return false;
        String calcule = hashMotDePasse(motEnClair);
        return constantTimeEquals(motDePasseHash, calcule);
    }

    public void changerMotDePasse(String nouveauHash) {
        this.motDePasseHash = Objects.requireNonNull(nouveauHash, "nouveauHash ne doit pas être null");
    }

    public void changerMotDePasseEnClair(String nouveauMotDePasse) {
        this.motDePasseHash = hashMotDePasse(nouveauMotDePasse);
    }

    public boolean hasRole(Role r) {
        return this.role == r;
    }

    public Utilisateur masquerDonneesSensibles() {
        Utilisateur u = new Utilisateur(this.id, this.email, "***", this.role, this.prenom, this.nom, this.departement, this.dateNaissance);
        u.setCode(this.code);
        u.setCodePermanent(this.codePermanent);
        return u;
    }

    // ===================== Utils hash =====================
    public static String hashMotDePasse(String motEnClair) {
        if (motEnClair == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(motEnClair.getBytes(StandardCharsets.UTF_8));
            return toHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int res = 0;
        for (int i = 0; i < a.length(); i++) res |= a.charAt(i) ^ b.charAt(i);
        return res == 0;
    }

    private static String normaliserTexte(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ===================== Getters/Setters =====================
    public UUID getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = (code == null || code.isBlank()) ? null : code.trim().toUpperCase(); }

    public String getCodePermanent() { return codePermanent; }
    public void setCodePermanent(String codePermanent) {
        this.codePermanent = (codePermanent == null || codePermanent.isBlank()) ? null : codePermanent.trim().toUpperCase();
    }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = normaliserTexte(prenom); }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = normaliserTexte(nom); }

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = normaliserTexte(departement); }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
