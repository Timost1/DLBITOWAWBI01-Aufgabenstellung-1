package de.iu.fallstudie.projektportal.web;

import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import de.iu.fallstudie.projektportal.service.BenutzerService;
import de.iu.fallstudie.projektportal.web.dto.BenutzerAendernDto;
import de.iu.fallstudie.projektportal.web.dto.BenutzerAnlegenDto;
import de.iu.fallstudie.projektportal.web.dto.BenutzerDto;
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
 * REST-Ressource "Benutzerkonto" - ausschliesslich fuer Administratoren.
 *
 * <p>Der Zugriffsschutz ist doppelt verankert: einmal als Pfadregel in
 * {@code SecurityConfig} ({@code /api/benutzer/**} erfordert die Rolle ADMIN)
 * und zusaetzlich deklarativ ueber {@code @PreAuthorize} an der Klasse. Die
 * Redundanz ist beabsichtigt: Wird die Pfadregel spaeter umgebaut, bleibt der
 * Schutz am Endpunkt bestehen.</p>
 */
@RestController
@RequestMapping("/api/benutzer")
@PreAuthorize("hasRole('ADMIN')")
public class BenutzerController {

    private final BenutzerService benutzerService;

    public BenutzerController(BenutzerService benutzerService) {
        this.benutzerService = benutzerService;
    }

    /** GET /api/benutzer - alle Konten des eigenen Mandanten. */
    @GetMapping
    public List<BenutzerDto> alle(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet) {
        return benutzerService.findeAlle(angemeldet);
    }

    /** GET /api/benutzer/{id} */
    @GetMapping("/{id}")
    public BenutzerDto einzelnes(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                                 @PathVariable Long id) {
        return benutzerService.findeEinen(angemeldet, id);
    }

    /** POST /api/benutzer - Konto anlegen und Rollen vergeben. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BenutzerDto anlegen(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                               @Valid @RequestBody BenutzerAnlegenDto eingabe) {
        return benutzerService.anlegen(angemeldet, eingabe);
    }

    /** PUT /api/benutzer/{id} - Name, Rollen, Aktivstatus und optional Kennwort. */
    @PutMapping("/{id}")
    public BenutzerDto aendern(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet,
                               @PathVariable Long id,
                               @Valid @RequestBody BenutzerAendernDto eingabe) {
        return benutzerService.aendern(angemeldet, id, eingabe);
    }
}
