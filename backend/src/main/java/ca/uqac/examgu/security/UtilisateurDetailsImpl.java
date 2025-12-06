package ca.uqac.examgu.security;

import ca.uqac.examgu.model.Utilisateur;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

public class UtilisateurDetailsImpl implements UserDetails {

    private final Utilisateur utilisateur;

    public UtilisateurDetailsImpl(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+utilisateur.getRole().name()));
    }

    @Override
    public String getPassword() {
        return utilisateur.getPassword();
    }

    @Override
    public String getUsername() {
        return utilisateur.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
