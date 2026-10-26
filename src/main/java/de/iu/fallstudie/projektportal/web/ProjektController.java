package de.iu.fallstudie.projektportal.web;

import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import de.iu.fallstudie.projektportal.service.ProjektService;
import de.iu.fallstudie.projektportal.web.dto.ProjektDto;
import de.iu.fallstudie.projektportal.web.dto.ProjektEingabeDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-Ressource "Projekt".
 *
 * <p>Der Controller bleibt eine duenne Schicht: Er nimmt HTTP-Anfragen an,
 * laesst die Eingaben von Bean Validation pruefen ({@code @Valid}), delegiert
 * an {@link ProjektService} und gibt DTOs zurueck, die Spring als JSON
 * serialisiert. Die grobgranulare Rollenpruefung erfolgt deklarativ ueber
 * {@code @PreAuthorize}.</p>
 */
@RestController
@RequestMapping("/api/projekte")
public class ProjektController {

    private final ProjektService projektService;

    public ProjektController(ProjektService projektService) {
        this.projektService = projektService;
    }

    /** GET /api/projekte - alle fuer den Benutzer sichtbaren Projekte. */
    @GetMapping
    public List<ProjektDto> alleSichtbaren(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet) {
        return projektService.findeSichtbareProjekte(angemeldet);
    }

    /** GET /api/projekte/{id} - Detaildaten eines Projekts. */
    @GetMapping("/{id}")
    public ProjektDto einzelnes(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                                @PathVariable Long id) {
        return projektService.findeProjekt(angemeldet, id);
    }

    /** POST /api/projekte - neues Projekt anlegen. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROJEKTLEITER','ADMIN')")
    public ProjektDto anlegen(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                              @Valid @RequestBody ProjektEingabeDto eingabe) {
        return projektService.anlegen(angemeldet, eingabe);
    }

    /** PUT /api/projekte/{id} - Stammdaten und Mitarbeiterzuordnung ersetzen. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROJEKTLEITER','ADMIN')")
    public ProjektDto aendern(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                              @PathVariable Long id,
                              @Valid @RequestBody ProjektEingabeDto eingabe) {
        return projektService.aendern(angemeldet, id, eingabe);
    }

    /** POST /api/projekte/{id}/archivierung - Projekt archivieren. */
    @PostMapping("/{id}/archivierung")
    @PreAuthorize("hasAnyRole('PROJEKTLEITER','ADMIN')")
    public ProjektDto archivieren(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                                  @PathVariable Long id) {
        return projektService.archivieren(angemeldet, id);
    }

    /** POST /api/projekte/{id}/reaktivierung - Archivierung zuruecknehmen. */
    @PostMapping("/{id}/reaktivierung")
    @PreAuthorize("hasAnyRole('PROJEKTLEITER','ADMIN')")
    public ProjektDto reaktivieren(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                                   @PathVariable Long id) {
        return projektService.reaktivieren(angemeldet, id);
    }
}
