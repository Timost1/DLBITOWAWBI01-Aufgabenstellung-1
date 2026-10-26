package de.iu.fallstudie.projektportal.service;

import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;
import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Projekt;
import de.iu.fallstudie.projektportal.domain.Rolle;
import de.iu.fallstudie.projektportal.repository.AufgabeRepository;
import de.iu.fallstudie.projektportal.repository.BenutzerRepository;
import de.iu.fallstudie.projektportal.repository.ProjektRepository;
import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import de.iu.fallstudie.projektportal.web.dto.FortschrittDto;
import de.iu.fallstudie.projektportal.web.dto.ProjektDto;
import de.iu.fallstudie.projektportal.web.dto.ProjektEingabeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fachlogik der Projektverwaltung.
 *
 * <p>Die Serviceschicht ist die einzige Stelle, an der Geschaeftsregeln
 * implementiert sind. Controller uebersetzen lediglich HTTP in Methodenaufrufe,
 * Repositories kapseln den Datenbankzugriff. Diese Aufteilung entspricht der
 * Anwendungs- bzw. Logikschicht des Drei-Schichten-Modells.</p>
 */
@Service
@Transactional(readOnly = true)
public class ProjektService {

    private final ProjektRepository projektRepository;
    private final BenutzerRepository benutzerRepository;
    private final AufgabeRepository aufgabeRepository;
    private final ZugriffskontrollService zugriffskontrolle;

    public ProjektService(ProjektRepository projektRepository,
                          BenutzerRepository benutzerRepository,
                          AufgabeRepository aufgabeRepository,
                          ZugriffskontrollService zugriffskontrolle) {
        this.projektRepository = projektRepository;
        this.benutzerRepository = benutzerRepository;
        this.aufgabeRepository = aufgabeRepository;
        this.zugriffskontrolle = zugriffskontrolle;
    }

    /**
     * Liefert die fuer den angemeldeten Benutzer sichtbaren Projekte.
     *
     * <p>Administratoren sehen alle Projekte ihres Mandanten, alle anderen
     * Rollen nur jene, in denen sie Projektleitung oder zugeordnetes Mitglied
     * sind. Damit setzt die Methode die User Story "nur die Projekte sehen, fuer
     * die ich berechtigt bin" um. Die Einschraenkung erfolgt in der Abfrage und
     * nicht durch nachtraegliches Filtern - so verlaesst kein unberechtigter
     * Datensatz die Datenbank.</p>
     */
    public List<ProjektDto> findeSichtbareProjekte(AngemeldeterBenutzer angemeldet) {
        List<Projekt> projekte = zugriffskontrolle.istAdmin(angemeldet)
                ? projektRepository.findByMandantIdOrderByNameAsc(angemeldet.getMandantId())
                : projektRepository.findeBeteiligteProjekte(
                        angemeldet.getMandantId(), angemeldet.getBenutzerId());

        return projekte.stream()
                .map(projekt -> zuDto(projekt, angemeldet))
                .toList();
    }

    public ProjektDto findeProjekt(AngemeldeterBenutzer angemeldet, Long projektId) {
        Projekt projekt = zugriffskontrolle.ladeProjektMitLeserecht(angemeldet, projektId);
        return zuDto(projekt, angemeldet);
    }

    /**
     * Legt ein Projekt an. Die aufrufende Rolle wird bereits im Controller
     * deklarativ geprueft; hier folgen die fachlichen Regeln.
     */
    @Transactional
    public ProjektDto anlegen(AngemeldeterBenutzer angemeldet, ProjektEingabeDto eingabe) {
        pruefeZeitraum(eingabe);
        pruefeNamenseindeutigkeit(angemeldet.getMandantId(), eingabe.name(), null);

        Benutzer leiter = ladeMitarbeitendenDesMandanten(angemeldet, eingabe.leiterId());
        if (!leiter.hatRolle(Rolle.PROJEKTLEITER) && !leiter.hatRolle(Rolle.ADMIN)) {
            throw new FachlicheRegelException(
                    "Als Projektleitung können nur Konten mit der Rolle Projektleiter:in eingetragen werden.");
        }

        Projekt projekt = new Projekt(leiter.getMandant(), eingabe.name().trim(), leiter);
        uebernehmeStammdaten(projekt, eingabe, angemeldet);
        return zuDto(projektRepository.save(projekt), angemeldet);
    }

    @Transactional
    public ProjektDto aendern(AngemeldeterBenutzer angemeldet, Long projektId, ProjektEingabeDto eingabe) {
        Projekt projekt = zugriffskontrolle.ladeProjektMitVerwaltungsrecht(angemeldet, projektId);
        if (projekt.istArchiviert()) {
            throw new FachlicheRegelException(
                    "Ein archiviertes Projekt kann nicht bearbeitet werden. Bitte zuvor reaktivieren.");
        }
        pruefeZeitraum(eingabe);
        pruefeNamenseindeutigkeit(angemeldet.getMandantId(), eingabe.name(), projekt);

        Benutzer leiter = ladeMitarbeitendenDesMandanten(angemeldet, eingabe.leiterId());
        if (!leiter.hatRolle(Rolle.PROJEKTLEITER) && !leiter.hatRolle(Rolle.ADMIN)) {
            throw new FachlicheRegelException(
                    "Als Projektleitung können nur Konten mit der Rolle Projektleiter:in eingetragen werden.");
        }

        projekt.setName(eingabe.name().trim());
        projekt.setLeiter(leiter);
        uebernehmeStammdaten(projekt, eingabe, angemeldet);
        return zuDto(projekt, angemeldet);
    }

    /**
     * Archiviert ein Projekt. Archivierung statt Loeschung erhaelt die Historie
     * abgeschlossener Kundenprojekte und ist die im Product Backlog geforderte
     * Operation.
     */
    @Transactional
    public ProjektDto archivieren(AngemeldeterBenutzer angemeldet, Long projektId) {
        Projekt projekt = zugriffskontrolle.ladeProjektMitVerwaltungsrecht(angemeldet, projektId);
        if (projekt.istArchiviert()) {
            throw new FachlicheRegelException("Das Projekt ist bereits archiviert.");
        }
        projekt.archivieren();
        return zuDto(projekt, angemeldet);
    }

    @Transactional
    public ProjektDto reaktivieren(AngemeldeterBenutzer angemeldet, Long projektId) {
        Projekt projekt = zugriffskontrolle.ladeProjektMitVerwaltungsrecht(angemeldet, projektId);
        if (!projekt.istArchiviert()) {
            throw new FachlicheRegelException("Das Projekt ist nicht archiviert.");
        }
        projekt.reaktivieren();
        return zuDto(projekt, angemeldet);
    }

    /**
     * Berechnet den Fortschritt eines Projekts als Anteil erledigter Aufgaben.
     * Die Zaehlung erfolgt per Aggregatabfrage in der Datenbank und nicht durch
     * Laden aller Aufgaben in den Anwendungsspeicher.
     */
    public FortschrittDto berechneFortschritt(Long projektId) {
        long gesamt = aufgabeRepository.countByProjektId(projektId);
        long offen = aufgabeRepository.countByProjektIdAndStatus(projektId, Aufgabenstatus.OFFEN);
        long inBearbeitung = aufgabeRepository
                .countByProjektIdAndStatus(projektId, Aufgabenstatus.IN_BEARBEITUNG);
        long erledigt = aufgabeRepository
                .countByProjektIdAndStatus(projektId, Aufgabenstatus.ERLEDIGT);
        return FortschrittDto.berechnen(gesamt, offen, inBearbeitung, erledigt);
    }

    private void uebernehmeStammdaten(Projekt projekt, ProjektEingabeDto eingabe,
                                      AngemeldeterBenutzer angemeldet) {
        projekt.setKunde(leerZuNull(eingabe.kunde()));
        projekt.setBeschreibung(leerZuNull(eingabe.beschreibung()));
        projekt.setStartDatum(eingabe.startDatum());
        projekt.setEndDatum(eingabe.endDatum());
        projekt.setMitglieder(ladeMitglieder(angemeldet, eingabe.mitgliedIds()));
    }

    /**
     * Loest die uebergebenen Mitglieds-IDs zu Benutzern desselben Mandanten auf.
     * Eine ID aus einem fremden Mandanten fuehrt zu HTTP 404 und nicht etwa zu
     * einer stillen Zuordnung.
     */
    private Set<Benutzer> ladeMitglieder(AngemeldeterBenutzer angemeldet, Set<Long> mitgliedIds) {
        Set<Benutzer> mitglieder = new LinkedHashSet<>();
        if (mitgliedIds == null) {
            return mitglieder;
        }
        for (Long id : mitgliedIds) {
            mitglieder.add(ladeMitarbeitendenDesMandanten(angemeldet, id));
        }
        return mitglieder;
    }

    private Benutzer ladeMitarbeitendenDesMandanten(AngemeldeterBenutzer angemeldet, Long benutzerId) {
        Benutzer benutzer = benutzerRepository
                .findByIdAndMandantId(benutzerId, angemeldet.getMandantId())
                .orElseThrow(() -> new RessourceNichtGefundenException(
                        "Benutzerkonto " + benutzerId + " wurde nicht gefunden."));
        if (!benutzer.isAktiv()) {
            throw new FachlicheRegelException(
                    "Das Konto von " + benutzer.getVollerName() + " ist deaktiviert "
                            + "und kann keinem Projekt zugeordnet werden.");
        }
        return benutzer;
    }

    private void pruefeZeitraum(ProjektEingabeDto eingabe) {
        if (eingabe.startDatum() != null && eingabe.endDatum() != null
                && eingabe.endDatum().isBefore(eingabe.startDatum())) {
            throw new FachlicheRegelException("Das Enddatum darf nicht vor dem Startdatum liegen.");
        }
    }

    private void pruefeNamenseindeutigkeit(Long mandantId, String name, Projekt bestehend) {
        String bereinigt = name.trim();
        boolean unveraendert = bestehend != null && bestehend.getName().equalsIgnoreCase(bereinigt);
        if (!unveraendert
                && projektRepository.existsByMandantIdAndNameIgnoreCase(mandantId, bereinigt)) {
            throw new FachlicheRegelException(
                    "Ein Projekt mit dem Namen \"" + bereinigt + "\" existiert bereits.");
        }
    }

    private String leerZuNull(String wert) {
        return wert == null || wert.isBlank() ? null : wert.trim();
    }

    private ProjektDto zuDto(Projekt projekt, AngemeldeterBenutzer angemeldet) {
        boolean darfBearbeiten = !projekt.istArchiviert()
                && (zugriffskontrolle.istAdmin(angemeldet)
                    || projekt.getLeiter().getId().equals(angemeldet.getBenutzerId()));
        return ProjektDto.von(projekt, berechneFortschritt(projekt.getId()), darfBearbeiten);
    }
}
