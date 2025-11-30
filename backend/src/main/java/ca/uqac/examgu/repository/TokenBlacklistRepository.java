package ca.uqac.examgu.repository;

import ca.uqac.examgu.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    Optional<TokenBlacklist> findByToken(String token);

    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.dateExpiration < :now")
    void supprimerTokensExpires(@Param("now") LocalDateTime now);

    List<TokenBlacklist> findByEmail(String email);

}