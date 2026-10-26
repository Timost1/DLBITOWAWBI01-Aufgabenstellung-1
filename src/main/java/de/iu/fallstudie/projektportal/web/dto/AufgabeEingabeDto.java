package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Eingabe-DTO zum Anlegen und Bearbeiten einer Aufgabe. */
public record AufgabeEingabeDto(

        @NotBlank(message = "Der Aufgabentitel ist ein Pflichtfeld.")
        @Size(max = 160, message = "Der Aufgabentitel darf höchstens 160 Zeichen lang sein.")
        String titel,

        @Size(max = 2000, message = "Die Beschreibung darf höchstens 2000 Zeichen lang sein.")
        String beschreibung,

        @NotNull(message = "Der Status ist anzugeben.")
        Aufgabenstatus status,

        Long bearbeiterId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate faelligAm
) {
}
