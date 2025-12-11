package ca.uqac.examgu.security;

import ca.uqac.examgu.service.TokenBlacklistService;
import ca.uqac.examgu.service.UtilisateurDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final UtilisateurDetailsServiceImpl utilisateurService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtFilter(UtilisateurDetailsServiceImpl utilisateurService, JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.utilisateurService = utilisateurService;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        if (path.startsWith("/api/auth/") || path.equals("/health") || path.equals("/docs")) {
            System.out.println("JwtFilter: Skipping authentication for " + path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String email = null;
        boolean isTokenExpired = true;

        final String authorization = request.getHeader("Authorization");

        if(authorization != null && authorization.startsWith("Bearer ")){
            token = authorization.substring(7);

            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token invalide");
                return;
            }

            try {
                isTokenExpired = jwtService.isTokenExpired(token);
                email = jwtService.extractEmail(token);

            } catch (Exception e) {
                System.out.println("Error processing token: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token invalide");
                return;
            }
        }

        if(!isTokenExpired && email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = utilisateurService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } catch (UsernameNotFoundException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Utilisateur non trouvé");
                return;
            }
        } else {
            System.out.println("Token validation failed - expired: " + isTokenExpired + ", email: " + email + ", auth: " + SecurityContextHolder.getContext().getAuthentication());
        }

        filterChain.doFilter(request, response);
    }
}