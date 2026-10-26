package de.iu.fallstudie.projektportal.service;

import de.iu.fallstudie.projektportal.domain.Aufgabe;
import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Projekt;
import de.iu.fallstudie.projektportal.repository.AufgabeRepository;
import de.iu.fallstudie.projektportal.repository.BenutzerRepository;
import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import de.iu.fallstudie.projektportal.web.dto.AufgabeDto;
import de.iu.fallstudie.projektportal.web.dto.AufgabeEingabeDto;
import de.iu.fallstudie.projektportal.web.dto.StatusEingabeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fachlogik der Aufgabenverwaltung.
 *
 * <p>Aufgaben sind Teil des Aggregats Projekt. Jeder Zugriff laeuft daher
 * ueber das Projekt und dessen Berechtigungspruefung; eine Aufgabe ist nie
 * ohne ihr Projekt adressierbar.</p>
 */
@Service
@Transactional(readOnly = true)
public class AufgabeService {

    private final AufgabeRepository aufgabeRepository;
    private final BenutzerRepository benutzerRepository;
    private final ZugriffskontrollService zugriffskontrolle;

    public AufgabeService(AufgabeRepository aufgabeRepository,
                          BenutzerRepository benutzerRepository,
                          ZugriffskontrollService zugriffskontrolle) {
        this.aufgabeRepository = aufgabeRepository;
        this.benutzerRepository = benutzerRepository;
        this.zugriffskontrolle = zugriffskontrolle;
    }

    public List<AufgabeDto> findeAufgabenZuProjekt(AngemeldeterBenutzer angemeldet, Long projektId) {
        Projekt projekt = zugriffskontrolle.ladeProjektMitLeserecht(angemeldet, projektId);
        return aufgabeRepository
                .findByProjektIdOrderByStatusAscFaelligAmAsc(projekt.getId())
                .stream()
                .map(AufgabeDto::von)
                .toList();
    }

    @Transactional
    public AufgabeDto anlegen(AngemeldeterBenutzer angemeldet, Long projektId, AufgabeEingabeDto eingabe) {
        Projekt projekt = zugriffskontrolle.ladeProjektMitAufgabenrecht(angemeldet, projektId);

        Aufgabe aufgabe = new Aufgabe(projekt, eingabe.titel().trim());
        uebernehmeFelder(aufgabe, eingabe, projekt, angemeldet);
        return AufgabeDto.von(aufgabeRepository.save(aufgabe));
    }

    @Transactional
    public AufgabeDto aendern(AngemeldeterBenutzer angemeldet, Long aufgabeId, AufgabeEingabeDto eingabe) {
        Aufgabe aufgabe = ladeAufgabe(angemeldet, aufgabeId);
        Projekt projekt = zugriffskontrolle
                .ladeProjektMitAufgabenrecht(angemeldet, aufgabe.getProjekt().getId());

        aufgabe.setTitel(eingabe.titel().trim());
        uebernehmeFelder(aufgabe, eingabe, projekt, angemeldet);
        return AufgabeDto.von(aufgabe);
    }

    /**
     * Setzt den Status einer Aufgabe (offen, in Bearbeitung, erledigt). Der
     * schmale Endpunkt bildet den haeufigsten Vorgang ab und laesst alle
     * uebrigen Felder unberuehrt.
     */
    @Transactional
    public AufgabeDto statusAendern(AngemeldeterBenutzer angemeldet, Long aufgabeId, StatusEingabeDto eingabe) {
        Aufgabe aufgabe = ladeAufgabe(angemeldet, aufgabeId);
        zugriffskontrolle.ladeProjektMitAufgabenrecht(angemeldet, aufgabe.getProjekt().getId());
        aufgabe.setStatus(eingabe.status());
        return AufgabeDto.von(aufgabe);
    }

    @Transactional
    public void loeschen(AngemeldeterBenutzer angemeldet, Long aufgabeId) {
        Aufgabe aufgabe = ladeAufgabe(angemeldet, aufgabeId);
        zugriffskontrolle.ladeProjektMitAufgabenrecht(angemeldet, aufgabe.getProjekt().getId());
        aufgabeRepository.delete(aufgabe);
    }

    private void uebernehmeFelder(Aufgabe aufgabe, AufgabeEingabeDto eingabe,
                                  Projekt projekt, AngemeldeterBenutzer angemeldet) {
        aufgabe.setBeschreibung(eingabe.beschreibung() == null || eingabe.beschreibung().isBlank()
                ? null : eingabe.beschreibung().trim());
        aufgabe.setStatus(eingabe.status());
        aufgabe.setFaelligAm(eingabe.faelligAm());
        aufgabe.setBearbeiter(ermittleBearbeiter(eingabe.bearbeiterId(), projekt, angemeldet));
    }

    /**
     * Ein Bearbeiter muss dem Projekt zugeordnet sein. Andernfalls entstuenden
     * Aufgaben, die ihr Bearbeiter selbst nicht sehen kann.
     */
    private Benutzer ermittleBearbeiter(Long bearbeiterId, Projekt projekt,
                                        AngemeldeterBenutzer angemeldet) {
        if (bearbeiterId == null) {
            return null;
        }
        Benutzer bearbeiter = benutzerRepository
                .findByIdAndMandantId(bearbeiterId, angemeldet.getMandantId())
                .orElseThrow(() -> new RessourceNichtGefundenException(
                        "Benutzerkonto " + bearbeiterId + " wurde nicht gefunden."));
        if (!projekt.istBeteiligt(bearbeiter)) {
            throw new FachlicheRegelException(
                    "Eine Aufgabe kann nur an Mitarbeitende vergeben werden, "
                            + "die dem Projekt zugeordnet sind.");
        }
        return bearbeiter;
    }

    private Aufgabe ladeAufgabe(AngemeldeterBenutzer angemeldet, Long aufgabeId) {
        return aufgabeRepository
                .findByIdAndProjektMandantId(aufgabeId, angemeldet.getMandantId())
                .orElseThrow(() -> new RessourceNichtGefundenException("Aufgabe nicht gefunden."));
    }
}
