package de.iu.fallstudie.projektportal.web;

import de.iu.fallstudie.projektportal.service.ZugriffskontrollService;
import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import de.iu.fallstudie.projektportal.web.dto.AnmeldungDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpunkte rund um die Sitzung des Benutzers.
 *
 * <p>Die eigentliche Pruefung der Anmeldedaten uebernimmt Spring Security
 * ueber HTTP Basic, bevor diese Controller-Methoden erreicht werden. Der
 * Endpunkt {@code /api/auth/login} bestaetigt die Anmeldung daher lediglich und
 * liefert das Profil des Benutzers zurueck. Die Abmeldung ist in
 * {@code SecurityConfig} als Spring-Security-Logout auf {@code /api/auth/logout}
 * konfiguriert und invalidiert die Session serverseitig.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ZugriffskontrollService zugriffskontrolle;

    public AuthController(ZugriffskontrollService zugriffskontrolle) {
        this.zugriffskontrolle = zugriffskontrolle;
    }

    /** Bestaetigt eine erfolgreiche Anmeldung (HTTP Basic). */
    @PostMapping("/login")
    public AnmeldungDto anmelden(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet) {
        return AnmeldungDto.von(zugriffskontrolle.ladeAngemeldetenBenutzer(angemeldet));
    }

    /**
     * Liefert das Profil der aktuellen Sitzung. Das Frontend ruft diesen
     * Endpunkt beim Start auf, um eine noch gueltige Session wiederherzustellen,
     * ohne erneut nach Anmeldedaten zu fragen.
     */
    @GetMapping("/sitzung")
    public AnmeldungDto sitzung(@AuthenticationPrincipal AngemeldeterBenutzer angemeldet) {
        return AnmeldungDto.von(zugriffskontrolle.ladeAngemeldetenBenutzer(angemeldet));
    }
}
