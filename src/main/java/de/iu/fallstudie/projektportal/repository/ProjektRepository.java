package de.iu.fallstudie.projektportal.repository;

import de.iu.fallstudie.projektportal.domain.Projekt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository fuer Projekte.
 *
 * <p>Jede Methode fuehrt den Mandanten als Parameter mit. Damit ist die
 * Mandantentrennung bereits auf Ebene der Datenzugriffsschicht verankert und
 * nicht erst im Anwendungscode.</p>
 */
public interface ProjektRepository extends JpaRepository<Projekt, Long> {

    Optional<Projekt> findByIdAndMandantId(Long id, Long mandantId);

    List<Projekt> findByMandantIdOrderByNameAsc(Long mandantId);

    boolean existsByMandantIdAndNameIgnoreCase(Long mandantId, String name);

    /**
     * Projekte, an denen ein Benutzer fachlich beteiligt ist: entweder als
     * Projektleitung oder als zugeordnetes Mitglied. Diese Abfrage setzt die
     * User Story "nur die Projekte sehen, fuer die ich berechtigt bin" um.
     */
    @Query("""
            select distinct p from Projekt p
            left join p.mitglieder m
            where p.mandant.id = :mandantId
              and (p.leiter.id = :benutzerId or m.id = :benutzerId)
            order by p.name asc
            """)
    List<Projekt> findeBeteiligteProjekte(@Param("mandantId") Long mandantId,
                                         @Param("benutzerId") Long benutzerId);

    /** Wird geprueft, bevor ein Benutzerkonto deaktiviert oder umgerollt wird. */
    @Query("select count(p) from Projekt p where p.leiter.id = :benutzerId")
    long zaehleProjekteMitLeitung(@Param("benutzerId") Long benutzerId);
}
