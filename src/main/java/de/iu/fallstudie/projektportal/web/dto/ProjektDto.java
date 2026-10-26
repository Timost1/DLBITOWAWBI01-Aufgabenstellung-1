package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Projekt;
import de.iu.fallstudie.projektportal.domain.Projektstatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Ausgabe-DTO eines Projekts inklusive Fortschrittskennzahl.
 *
 * <p>Datumswerte werden im ISO-8601-Format serialisiert
 * ({@code YYYY-MM-DD}); die landesspezifische Formatierung uebernimmt das
 * Frontend anhand des Gebietsschemas des Browsers.</p>
 */
public record ProjektDto(
        Long id,
        String name,
        String kunde,
        String beschreibung,
        Projektstatus status,
        LocalDate startDatum,
        LocalDate endDatum,
        BenutzerKurzDto leiter,
        List<BenutzerKurzDto> mitglieder,
        FortschrittDto fortschritt,
        boolean darfBearbeiten
) {

    public static ProjektDto von(Projekt projekt, FortschrittDto fortschritt, boolean darfBearbeiten) {
        return new ProjektDto(
                projekt.getId(),
                projekt.getName(),
                projekt.getKunde(),
                projekt.getBeschreibung(),
                projekt.getStatus(),
                projekt.getStartDatum(),
                projekt.getEndDatum(),
                BenutzerKurzDto.von(projekt.getLeiter()),
                projekt.getMitglieder().stream()
                        .map(BenutzerKurzDto::von)
                        .sorted(Comparator.comparing(BenutzerKurzDto::vollerName))
                        .toList(),
                fortschritt,
                darfBearbeiten
        );
    }
}
