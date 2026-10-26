package de.iu.fallstudie.projektportal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Set;

/**
 * Eingabe-DTO zum Anlegen und Bearbeiten eines Projekts.
 *
 * <p>Die Menge {@code mitgliedIds} ersetzt beim Bearbeiten die vollstaendige
 * Zuordnung der Mitarbeitenden (PUT-Semantik). Ob die referenzierten IDs zum
 * eigenen Mandanten gehoeren, prueft die Serviceschicht - Bean Validation kann
 * nur die Struktur, nicht die fachliche Zulaessigkeit sicherstellen.</p>
 */
public record ProjektEingabeDto(

        @NotBlank(message = "Der Projektname ist ein Pflichtfeld.")
        @Size(max = 120, message = "Der Projektname darf höchstens 120 Zeichen lang sein.")
        String name,

        @Size(max = 120, message = "Der Kundenname darf höchstens 120 Zeichen lang sein.")
        String kunde,

        @Size(max = 2000, message = "Die Beschreibung darf höchstens 2000 Zeichen lang sein.")
        String beschreibung,

        @NotNull(message = "Es muss eine Projektleitung angegeben werden.")
        Long leiterId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDatum,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDatum,

        Set<Long> mitgliedIds
) {
}
