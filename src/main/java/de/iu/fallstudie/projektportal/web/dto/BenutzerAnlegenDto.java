package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Rolle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Eingabe-DTO zum Anlegen eines Benutzerkontos.
 *
 * <p>Die deklarativen Constraints werden von Bean Validation (Hibernate
 * Validator) geprueft, bevor der Controller die Serviceschicht aufruft.
 * Verstoesse fuehren zu HTTP 400 und erreichen die Geschaeftslogik nicht.</p>
 */
public record BenutzerAnlegenDto(

        @NotBlank(message = "Die E-Mail-Adresse ist ein Pflichtfeld.")
        @Email(message = "Die E-Mail-Adresse ist nicht gültig.")
        @Size(max = 160, message = "Die E-Mail-Adresse darf höchstens 160 Zeichen lang sein.")
        String email,

        @NotBlank(message = "Der Vorname ist ein Pflichtfeld.")
        @Size(max = 60, message = "Der Vorname darf höchstens 60 Zeichen lang sein.")
        String vorname,

        @NotBlank(message = "Der Nachname ist ein Pflichtfeld.")
        @Size(max = 60, message = "Der Nachname darf höchstens 60 Zeichen lang sein.")
        String nachname,

        @NotBlank(message = "Das Kennwort ist ein Pflichtfeld.")
        @Size(min = 10, max = 100, message = "Das Kennwort muss mindestens 10 Zeichen lang sein.")
        String passwort,

        @NotEmpty(message = "Es muss mindestens eine Rolle zugewiesen werden.")
        Set<Rolle> rollen
) {
}
