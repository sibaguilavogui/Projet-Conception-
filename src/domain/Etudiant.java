package domain;

import java.time.LocalDate;
import java.util.UUID;

public class Etudiant extends Utilisateur {

    // ✅ Constructeur utilisé par ton Main (avec infos pour codePermanent)
    public Etudiant(String email,
                    String motDePasseEnClair,
                    String prenom,
                    String nom,
                    String departement,
                    LocalDate dateNaissance) {
        super(email, motDePasseEnClair, Role.ETUDIANT, prenom, nom, departement, dateNaissance);
    }

    // ✅ Compatibilité (ancien)
    public Etudiant(String email, String motDePasseEnClair) {
        super(email, motDePasseEnClair, Role.ETUDIANT);
    }

    // ✅ Compatibilité (si tu construis depuis hash/UUID quelque part)
    public Etudiant(UUID id, String email, String motDePasseHash) {
        super(id, email, motDePasseHash, Role.ETUDIANT);
    }

    public Etudiant(UUID id,
                    String email,
                    String motDePasseHash,
                    String prenom,
                    String nom,
                    String departement,
                    LocalDate dateNaissance) {
        super(id, email, motDePasseHash, Role.ETUDIANT, prenom, nom, departement, dateNaissance);
    }
}
