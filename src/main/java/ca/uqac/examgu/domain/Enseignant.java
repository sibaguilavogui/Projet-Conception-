package ca.uqac.examgu.domain;

import java.time.LocalDate;
import java.util.UUID;

public class Enseignant extends Utilisateur {

    // ✅ Constructeur utilisé par ton Main (avec infos pour codePermanent)
    public Enseignant(String email,
                      String motDePasseEnClair,
                      String prenom,
                      String nom,
                      String departement,
                      LocalDate dateNaissance) {
        super(email, motDePasseEnClair, Role.ENSEIGNANT, prenom, nom, departement, dateNaissance);
    }

    // ✅ Compatibilité (ancien)
    public Enseignant(String email, String motDePasseEnClair) {
        super(email, motDePasseEnClair, Role.ENSEIGNANT);
    }

    // ✅ Compatibilité (si tu construis depuis hash/UUID quelque part)
    public Enseignant(UUID id, String email, String motDePasseHash) {
        super(id, email, motDePasseHash, Role.ENSEIGNANT);
    }

    public Enseignant(UUID id,
                      String email,
                      String motDePasseHash,
                      String prenom,
                      String nom,
                      String departement,
                      LocalDate dateNaissance) {
        super(id, email, motDePasseHash, Role.ENSEIGNANT, prenom, nom, departement, dateNaissance);
    }
}
