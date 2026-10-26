package de.iu.fallstudie.projektportal.repository;

import de.iu.fallstudie.projektportal.domain.Benutzer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository fuer Benutzerkonten.
 *
 * <p>Saemtliche Suchmethoden mit Ausnahme von {@link #findByEmail(String)}
 * sind auf einen Mandanten eingeschraenkt. {@code findByEmail} wird nur von der
 * Authentifizierung genutzt, bei der der Mandant noch nicht bekannt ist; die
 * E-Mail-Adresse ist dort systemweit eindeutig zu halten.</p>
 */
public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {

    Optional<Benutzer> findByEmail(String email);

    Optional<Benutzer> findByIdAndMandantId(Long id, Long mandantId);

    List<Benutzer> findByMandantIdOrderByNachnameAscVornameAsc(Long mandantId);

    boolean existsByEmail(String email);
}
