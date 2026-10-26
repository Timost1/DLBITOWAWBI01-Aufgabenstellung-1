package de.iu.fallstudie.projektportal.web;

import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import de.iu.fallstudie.projektportal.service.AufgabeService;
import de.iu.fallstudie.projektportal.web.dto.AufgabeDto;
import de.iu.fallstudie.projektportal.web.dto.AufgabeEingabeDto;
import de.iu.fallstudie.projektportal.web.dto.StatusEingabeDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-Ressource "Aufgabe".
 *
 * <p>Aufgaben sind eine Unterressource des Projekts. Das Anlegen und Auflisten
 * erfolgt daher unter {@code /api/projekte/{projektId}/aufgaben}, waehrend eine
 * bereits bestehende Aufgabe ueber ihre eigene ID adressiert wird. Diese
 * Ressourcenstruktur folgt den REST-Konventionen aus dem Lernskript.</p>
 *
 * <p>Fuer Aufgaben genuegt die Rolle Mitarbeiter:in; entscheidend ist nicht die
 * Rolle, sondern die Zuordnung zum Projekt. Diese feingranulare Pruefung
 * uebernimmt die Serviceschicht.</p>
 */
@RestController
@RequestMapping("/api")
public class AufgabeController {

    private final AufgabeService aufgabeService;

    public AufgabeController(AufgabeService aufgabeService) {
        this.aufgabeService = aufgabeService;
    }

    /** GET /api/projekte/{projektId}/aufgaben */
    @GetMapping("/projekte/{projektId}/aufgaben")
    public List<AufgabeDto> alleZuProjekt(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                                          @PathVariable Long projektId) {
        return aufgabeService.findeAufgabenZuProjekt(angemeldet, projektId);
    }

    /** POST /api/projekte/{projektId}/aufgaben */
    @PostMapping("/projekte/{projektId}/aufgaben")
    @ResponseStatus(HttpStatus.CREATED)
    public AufgabeDto anlegen(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                              @PathVariable Long projektId,
                              @Valid @RequestBody AufgabeEingabeDto eingabe) {
        return aufgabeService.anlegen(angemeldet, projektId, eingabe);
    }

    /** PUT /api/aufgaben/{id} */
    @PutMapping("/aufgaben/{id}")
    public AufgabeDto aendern(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                              @PathVariable Long id,
                              @Valid @RequestBody AufgabeEingabeDto eingabe) {
        return aufgabeService.aendern(angemeldet, id, eingabe);
    }

    /** PATCH /api/aufgaben/{id}/status - Statuswechsel offen/in Bearbeitung/erledigt. */
    @PatchMapping("/aufgaben/{id}/status")
    public AufgabeDto statusAendern(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                                    @PathVariable Long id,
                                    @Valid @RequestBody StatusEingabeDto eingabe) {
        return aufgabeService.statusAendern(angemeldet, id, eingabe);
    }

    /** DELETE /api/aufgaben/{id} */
    @DeleteMapping("/aufgaben/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void loeschen(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                         @PathVariable Long id) {
        aufgabeService.loeschen(angemeldet, id);
    }
}
