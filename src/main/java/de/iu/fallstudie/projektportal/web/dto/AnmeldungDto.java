package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Rolle;

import java.util.Comparator;
import java.util.List;

/**
 * Antwort auf eine erfolgreiche Anmeldung sowie auf die Abfrage der eigenen
 * Sitzung. Das Frontend leitet daraus ab, welche Navigationspunkte und Aktionen
 * sichtbar sind.
 */
public record AnmeldungDto(
        Long benutzerId,
        String email,
        String vollerName,
        List<Rolle> rollen,
        String mandantKuerzel,
        String mandantName
) {

    public static AnmeldungDto von(Benutzer benutzer) {
        return new AnmeldungDto(
                benutzer.getId(),
                benutzer.getEmail(),
                benutzer.getVollerName(),
                benutzer.getRollen().stream().sorted(Comparator.naturalOrder()).toList(),
                benutzer.getMandant().getKuerzel(),
                benutzer.getMandant().getName()
        );
    }
}
