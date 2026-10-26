package de.iu.fallstudie.projektportal.service;

import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Mandant;
import de.iu.fallstudie.projektportal.domain.Rolle;
import de.iu.fallstudie.projektportal.repository.BenutzerRepository;
import de.iu.fallstudie.projektportal.repository.MandantRepository;
import de.iu.fallstudie.projektportal.repository.ProjektRepository;
import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import de.iu.fallstudie.projektportal.web.dto.BenutzerAendernDto;
import de.iu.fallstudie.projektportal.web.dto.BenutzerAnlegenDto;
import de.iu.fallstudie.projektportal.web.dto.BenutzerDto;
import de.iu.fallstudie.projektportal.web.dto.BenutzerKurzDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fachlogik der Benutzer- und Rollenverwaltung.
 *
 * <p>Die Rollenpruefung (nur Administratoren) erfolgt deklarativ im Controller.
 * Dieser Service stellt darueber hinaus sicher, dass ein Mandant sich nicht
 * selbst handlungsunfaehig macht und dass Kennwoerter nur gehasht gespeichert
 * werden.</p>
 */
@Service
@Transactional(readOnly = true)
public class BenutzerService {

    private final BenutzerRepository benutzerRepository;
    private final MandantRepository mandantRepository;
    private final ProjektRepository projektRepository;
    private final PasswordEncoder passwordEncoder;

    public BenutzerService(BenutzerRepository benutzerRepository,
                           MandantRepository mandantRepository,
                           ProjektRepository projektRepository,
                           PasswordEncoder passwordEncoder) {
        this.benutzerRepository = benutzerRepository;
        this.mandantRepository = mandantRepository;
        this.projektRepository = projektRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<BenutzerDto> findeAlle(AngemeldeterBenutzer angemeldet) {
        return benutzerRepository
                .findByMandantIdOrderByNachnameAscVornameAsc(angemeldet.getMandantId())
                .stream()
                .map(BenutzerDto::von)
                .toList();
    }

    /**
     * Auswahlliste aktiver Konten des Mandanten. Dieser Endpunkt steht auch der
     * Projektleitung offen, weil sie Mitarbeitende zuordnen muss - allerdings
     * nur mit den reduzierten Feldern des {@link BenutzerKurzDto}.
     */
    public List<BenutzerKurzDto> findeAuswahlliste(AngemeldeterBenutzer angemeldet) {
        return benutzerRepository
                .findByMandantIdOrderByNachnameAscVornameAsc(angemeldet.getMandantId())
                .stream()
                .filter(Benutzer::isAktiv)
                .map(BenutzerKurzDto::von)
                .toList();
    }

    public BenutzerDto findeEinen(AngemeldeterBenutzer angemeldet, Long benutzerId) {
        return BenutzerDto.von(ladeImMandanten(angemeldet, benutzerId));
    }

    @Transactional
    public BenutzerDto anlegen(AngemeldeterBenutzer angemeldet, BenutzerAnlegenDto eingabe) {
        String email = eingabe.email().trim().toLowerCase();
        if (benutzerRepository.existsByEmail(email)) {
            throw new FachlicheRegelException(
                    "Zu dieser E-Mail-Adresse existiert bereits ein Benutzerkonto.");
        }
        Mandant mandant = mandantRepository.findById(angemeldet.getMandantId())
                .orElseThrow(() -> new RessourceNichtGefundenException("Mandant nicht gefunden."));

        Benutzer benutzer = new Benutzer(
                mandant,
                email,
                passwordEncoder.encode(eingabe.passwort()),
                eingabe.vorname().trim(),
                eingabe.nachname().trim(),
                eingabe.rollen());
        return BenutzerDto.von(benutzerRepository.save(benutzer));
    }

    @Transactional
    public BenutzerDto aendern(AngemeldeterBenutzer angemeldet, Long benutzerId,
                               BenutzerAendernDto eingabe) {
        Benutzer benutzer = ladeImMandanten(angemeldet, benutzerId);
        boolean betrifftEigenesKonto = benutzerId.equals(angemeldet.getBenutzerId());

        // Schutz vor Selbstaussperrung: Administratoren duerfen sich weder die
        // Administratorrolle entziehen noch das eigene Konto deaktivieren.
        if (betrifftEigenesKonto && !eingabe.rollen().contains(Rolle.ADMIN)) {
            throw new FachlicheRegelException(
                    "Die eigene Administratorrolle kann nicht entzogen werden.");
        }
        if (betrifftEigenesKonto && !eingabe.aktiv()) {
            throw new FachlicheRegelException("Das eigene Konto kann nicht deaktiviert werden.");
        }

        // Wer Projekte leitet, muss die Rolle Projektleiter:in behalten,
        // sonst entstuenden Projekte ohne berechtigte Leitung.
        boolean verliertProjektleitung = !eingabe.rollen().contains(Rolle.PROJEKTLEITER)
                && !eingabe.rollen().contains(Rolle.ADMIN);
        if (verliertProjektleitung && projektRepository.zaehleProjekteMitLeitung(benutzerId) > 0) {
            throw new FachlicheRegelException(
                    "Das Konto leitet noch Projekte. Bitte zuerst die Projektleitung übertragen.");
        }

        benutzer.setVorname(eingabe.vorname().trim());
        benutzer.setNachname(eingabe.nachname().trim());
        benutzer.setRollen(eingabe.rollen());
        benutzer.setAktiv(eingabe.aktiv());
        if (eingabe.neuesPasswort() != null && !eingabe.neuesPasswort().isBlank()) {
            benutzer.setPasswortHash(passwordEncoder.encode(eingabe.neuesPasswort()));
        }
        return BenutzerDto.von(benutzer);
    }

    private Benutzer ladeImMandanten(AngemeldeterBenutzer angemeldet, Long benutzerId) {
        return benutzerRepository.findByIdAndMandantId(benutzerId, angemeldet.getMandantId())
                .orElseThrow(() -> new RessourceNichtGefundenException(
                        "Benutzerkonto nicht gefunden."));
    }
}
