package de.iu.fallstudie.projektportal.service;

import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Projekt;
import de.iu.fallstudie.projektportal.domain.Rolle;
import de.iu.fallstudie.projektportal.repository.BenutzerRepository;
import de.iu.fallstudie.projektportal.repository.ProjektRepository;
import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buendelt die datensatzbezogene Autorisierung an einer Stelle.
 *
 * <p>Spring Security prueft ueber Pfadregeln und {@code @PreAuthorize} nur, ob
 * eine Rolle grundsaetzlich zu einem Endpunkt passt. Ob ein konkreter Benutzer
 * auf ein konkretes Projekt zugreifen darf, ist dagegen eine fachliche Frage.
 * Diese Klasse beantwortet sie zentral, damit die Regel nicht in jedem Service
 * erneut - und potenziell abweichend - implementiert wird.</p>
 *
 * <p>Jede Methode zieht den Mandanten ausschliesslich aus dem
 * Sicherheitskontext. Eine Anfrage kann den Mandanten daher nicht beeinflussen.</p>
 */
@Service
@Transactional(readOnly = true)
public class ZugriffskontrollService {

    private final BenutzerRepository benutzerRepository;
    private final ProjektRepository projektRepository;

    public ZugriffskontrollService(BenutzerRepository benutzerRepository,
                                   ProjektRepository projektRepository) {
        this.benutzerRepository = benutzerRepository;
        this.projektRepository = projektRepository;
    }

    /** Laedt den angemeldeten Benutzer als Domaenenobjekt. */
    public Benutzer ladeAngemeldetenBenutzer(AngemeldeterBenutzer angemeldet) {
        return benutzerRepository
                .findByIdAndMandantId(angemeldet.getBenutzerId(), angemeldet.getMandantId())
                .orElseThrow(() -> new RessourceNichtGefundenException(
                        "Das angemeldete Benutzerkonto existiert nicht mehr."));
    }

    /**
     * Laedt ein Projekt, auf das der Benutzer lesend zugreifen darf.
     *
     * <p>Zugriff haben Administratoren des Mandanten sowie alle fachlich
     * Beteiligten (Projektleitung und zugeordnete Mitarbeitende).</p>
     */
    public Projekt ladeProjektMitLeserecht(AngemeldeterBenutzer angemeldet, Long projektId) {
        Projekt projekt = ladeProjektImMandanten(angemeldet, projektId);
        if (darfLesen(angemeldet, projekt)) {
            return projekt;
        }
        // Kein 403, sondern 404: Die Existenz fremder Projekte bleibt verborgen.
        throw new RessourceNichtGefundenException("Projekt nicht gefunden.");
    }

    /**
     * Laedt ein Projekt, in dem der Benutzer Aufgaben anlegen und bearbeiten
     * darf. Voraussetzung ist Beteiligung am Projekt und ein nicht archivierter
     * Projektstatus.
     */
    public Projekt ladeProjektMitAufgabenrecht(AngemeldeterBenutzer angemeldet, Long projektId) {
        Projekt projekt = ladeProjektMitLeserecht(angemeldet, projektId);
        if (projekt.istArchiviert()) {
            throw new FachlicheRegelException(
                    "Das Projekt ist archiviert und kann nicht mehr bearbeitet werden.");
        }
        if (!istAdmin(angemeldet) && !projekt.istBeteiligt(ladeAngemeldetenBenutzer(angemeldet))) {
            throw new ZugriffVerweigertException(
                    "Nur dem Projekt zugeordnete Mitarbeitende dürfen dessen Aufgaben bearbeiten.");
        }
        return projekt;
    }

    /**
     * Laedt ein Projekt, dessen Stammdaten der Benutzer aendern darf. Das duerfen
     * die verantwortliche Projektleitung und Administratoren des Mandanten.
     */
    public Projekt ladeProjektMitVerwaltungsrecht(AngemeldeterBenutzer angemeldet, Long projektId) {
        Projekt projekt = ladeProjektImMandanten(angemeldet, projektId);
        boolean istVerantwortlich = projekt.getLeiter().getId().equals(angemeldet.getBenutzerId());
        if (istAdmin(angemeldet) || istVerantwortlich) {
            return projekt;
        }
        if (darfLesen(angemeldet, projekt)) {
            throw new ZugriffVerweigertException(
                    "Nur die verantwortliche Projektleitung darf die Projektdaten ändern.");
        }
        throw new RessourceNichtGefundenException("Projekt nicht gefunden.");
    }

    private Projekt ladeProjektImMandanten(AngemeldeterBenutzer angemeldet, Long projektId) {
        return projektRepository.findByIdAndMandantId(projektId, angemeldet.getMandantId())
                .orElseThrow(() -> new RessourceNichtGefundenException("Projekt nicht gefunden."));
    }

    private boolean darfLesen(AngemeldeterBenutzer angemeldet, Projekt projekt) {
        return istAdmin(angemeldet) || projekt.istBeteiligt(ladeAngemeldetenBenutzer(angemeldet));
    }

    public boolean istAdmin(AngemeldeterBenutzer angemeldet) {
        return hatRolle(angemeldet, Rolle.ADMIN);
    }

    public boolean hatRolle(AngemeldeterBenutzer angemeldet, Rolle rolle) {
        return angemeldet.getAuthorities().stream()
                .anyMatch(autoritaet -> autoritaet.getAuthority().equals("ROLE_" + rolle.name()));
    }
}
