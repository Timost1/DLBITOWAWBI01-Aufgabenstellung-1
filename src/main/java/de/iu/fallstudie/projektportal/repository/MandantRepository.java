package de.iu.fallstudie.projektportal.repository;

import de.iu.fallstudie.projektportal.domain.Mandant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring-Data-JPA-Repository fuer Mandanten. Die CRUD-Methoden stellt
 * {@link JpaRepository} bereit, die Abfragemethode wird aus dem Methodennamen
 * abgeleitet.
 */
public interface MandantRepository extends JpaRepository<Mandant, Long> {

    Optional<Mandant> findByKuerzel(String kuerzel);
}
