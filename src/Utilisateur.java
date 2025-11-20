package domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Utilisateur {

    private UUID id;
    private String email;
    private String motDePasseHash;
    private Role role;

    // Nouveaux attributs pertinents
    private String prenom;
    private String nom;
    private boolean actif = true;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereConnexion;
    private int tentativesConnexionEchouees;

    // --- Constructeurs ---

    public Utilisateur() {
        // requis par certains frameworks
        this.dateCreation = LocalDateTime.now();
    }

    public Utilisateur(UUID id,
                       String email,
                       String motDePasseHash,
                       Role role,
                       String prenom,
                       String nom) {
        this.id = id;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.role = role;
        this.prenom = prenom;
        this.nom = nom;
        this.actif = true;
        this.dateCreation = LocalDateTime.now();
    }

    // --- Méthodes métier ---

    public boolean verifierMotDePasse(String hashOuMotDePasse) {
        return motDePasseHash != null && motDePasseHash.equals(hashOuMotDePasse);
    }

    public void changerMotDePasse(String nouveauHash) {
        if (nouveauHash == null || nouveauHash.isBlank()) {
            throw new IllegalArgumentException("Le hash du mot de passe ne doit pas être vide");
        }
        this.motDePasseHash = nouveauHash;
        // reset des tentatives échouées après un changement de mot de passe
        this.tentativesConnexionEchouees = 0;
    }

    public boolean hasRole(Role role) {
        return this.role == role;
    }

    public Utilisateur masquerDonneesSensibles() {
        Utilisateur copie = new Utilisateur(
                this.id,
                this.email,
                null,
                this.role,
                this.prenom,
                this.nom
        );
        copie.actif = this.actif;
        copie.dateCreation = this.dateCreation;
        copie.dateDerniereConnexion = this.dateDerniereConnexion;
        return copie;
    }

    // --- Gestion du compte / sécurité ---

    public void enregistrerConnexionReussie(LocalDateTime now) {
        this.dateDerniereConnexion = now;
        this.tentativesConnexionEchouees = 0;
    }

    public void enregistrerConnexionEchouee() {
        this.tentativesConnexionEchouees++;
    }

    public boolean doitEtreVerrouille(int maxTentatives) {
        return this.tentativesConnexionEchouees >= maxTentatives;
    }

    public void desactiver() {
        this.actif = false;
    }

    public void activer() {
        this.actif = true;
        this.tentativesConnexionEchouees = 0;
    }

    public boolean estActif() {
        return actif;
    }

    public void changerEmail(String nouvelEmail) {
        if (nouvelEmail == null || !nouvelEmail.contains("@")) {
            throw new IllegalArgumentException("Email invalide");
        }
        this.email = nouvelEmail;
    }

    public String getNomComplet() {
        if (prenom == null || prenom.isBlank()) {
            return nom != null ? nom : "";
        }
        if (nom == null || nom.isBlank()) {
            return prenom;
        }
        return prenom + " " + nom;
    }

    // --- Getters / Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateDerniereConnexion() {
        return dateDerniereConnexion;
    }

    public void setDateDerniereConnexion(LocalDateTime dateDerniereConnexion) {
        this.dateDerniereConnexion = dateDerniereConnexion;
    }

    public int getTentativesConnexionEchouees() {
        return tentativesConnexionEchouees;
    }

    public void setTentativesConnexionEchouees(int tentativesConnexionEchouees) {
        this.tentativesConnexionEchouees = tentativesConnexionEchouees;
    }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Utilisateur)) return false;
        Utilisateur that = (Utilisateur) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Utilisateur{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", actif=" + actif +
                '}';
    }
}
