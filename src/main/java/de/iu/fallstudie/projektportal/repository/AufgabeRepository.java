package de.iu.fallstudie.projektportal.repository;

import de.iu.fallstudie.projektportal.domain.Aufgabe;
import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository fuer Aufgaben. Der Mandant wird ueber die Navigation
 * {@code projekt.mandant.id} mitgefuehrt, sodass auch hier kein Zugriff ohne
 * Mandantenbezug moeglich ist.
 */
public interface AufgabeRepository extends JpaRepository<Aufgabe, Long> {

    Optional<Aufgabe> findByIdAndProjektMandantId(Long id, Long mandantId);

    List<Aufgabe> findByProjektIdOrderByStatusAscFaelligAmAsc(Long projektId);

    long countByProjektId(Long projektId);

    long countByProjektIdAndStatus(Long projektId, Aufgabenstatus status);
}
