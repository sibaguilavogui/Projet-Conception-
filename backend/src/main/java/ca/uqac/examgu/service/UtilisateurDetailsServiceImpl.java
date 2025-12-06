package ca.uqac.examgu.service;

import ca.uqac.examgu.model.Utilisateur;
import ca.uqac.examgu.repository.UtilisateurRepository;
import ca.uqac.examgu.security.UtilisateurDetailsImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepo;

    public UtilisateurDetailsServiceImpl(UtilisateurRepository utilisateurRepo) {
        this.utilisateurRepo = utilisateurRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = this.utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        return new UtilisateurDetailsImpl(utilisateur);
    }

}
