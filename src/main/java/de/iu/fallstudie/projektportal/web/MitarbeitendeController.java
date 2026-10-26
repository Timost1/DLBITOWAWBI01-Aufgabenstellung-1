package de.iu.fallstudie.projektportal.web;

import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import de.iu.fallstudie.projektportal.service.BenutzerService;
import de.iu.fallstudie.projektportal.web.dto.BenutzerKurzDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Auswahlliste der aktiven Mitarbeitenden eines Mandanten.
 *
 * <p>Bewusst von {@code /api/benutzer} getrennt: Die Projektleitung muss
 * Mitarbeitende zuordnen und Aufgaben vergeben koennen, darf dafuer aber keinen
 * Zugriff auf die Benutzerverwaltung erhalten. Der Endpunkt liefert deshalb nur
 * ID, Name und E-Mail-Adresse - Rollen und Aktivstatus bleiben der
 * Administration vorbehalten (Prinzip der minimalen Rechte).</p>
 */
@RestController
@RequestMapping("/api/mitarbeitende")
public class MitarbeitendeController {

    private final BenutzerService benutzerService;

    public MitarbeitendeController(BenutzerService benutzerService) {
        this.benutzerService = benutzerService;
    }

    @GetMapping
    public List<BenutzerKurzDto> auswahlliste(
            @AuthenticationPrincipal AngemeldeterBenutzer angemeldet) {
        return benutzerService.findeAuswahlliste(angemeldet);
    }
}
