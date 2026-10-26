package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Rolle;

import java.util.Comparator;
import java.util.List;

/**
 * Ausgabe-DTO eines Benutzerkontos.
 *
 * <p>Der Kennwort-Hash wird nicht abgebildet. Die Trennung von Entity und DTO
 * verhindert, dass interne Felder ungewollt nach aussen gelangen.</p>
 */
public record BenutzerDto(
        Long id,
        String email,
        String vorname,
        String nachname,
        String vollerName,
        List<Rolle> rollen,
        boolean aktiv
) {

    public static BenutzerDto von(Benutzer benutzer) {
        return new BenutzerDto(
                benutzer.getId(),
                benutzer.getEmail(),
                benutzer.getVorname(),
                benutzer.getNachname(),
                benutzer.getVollerName(),
                benutzer.getRollen().stream().sorted(Comparator.naturalOrder()).toList(),
                benutzer.isAktiv()
        );
    }
}
