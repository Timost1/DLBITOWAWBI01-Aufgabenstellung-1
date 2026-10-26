package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Rolle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Eingabe-DTO zum Aendern eines Benutzerkontos (Name, Rollen, Aktivstatus).
 *
 * <p>Das Feld {@code neuesPasswort} ist optional: Bleibt es leer, wird das
 * bestehende Kennwort nicht angetastet. Die E-Mail-Adresse ist bewusst nicht
 * aenderbar, weil sie als stabiler Anmeldename dient.</p>
 */
public record BenutzerAendernDto(

        @NotBlank(message = "Der Vorname ist ein Pflichtfeld.")
        @Size(max = 60, message = "Der Vorname darf höchstens 60 Zeichen lang sein.")
        String vorname,

        @NotBlank(message = "Der Nachname ist ein Pflichtfeld.")
        @Size(max = 60, message = "Der Nachname darf höchstens 60 Zeichen lang sein.")
        String nachname,

        @NotEmpty(message = "Es muss mindestens eine Rolle zugewiesen werden.")
        Set<Rolle> rollen,

        @NotNull(message = "Der Aktivstatus ist anzugeben.")
        Boolean aktiv,

        @Size(min = 10, max = 100, message = "Das Kennwort muss mindestens 10 Zeichen lang sein.")
        String neuesPasswort
) {
}
