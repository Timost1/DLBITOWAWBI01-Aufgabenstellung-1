package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Aufgabe;
import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;

import java.time.Instant;
import java.time.LocalDate;

/** Ausgabe-DTO einer Aufgabe. */
public record AufgabeDto(
        Long id,
        Long projektId,
        String titel,
        String beschreibung,
        Aufgabenstatus status,
        BenutzerKurzDto bearbeiter,
        LocalDate faelligAm,
        Instant erstelltAm
) {

    public static AufgabeDto von(Aufgabe aufgabe) {
        return new AufgabeDto(
                aufgabe.getId(),
                aufgabe.getProjekt().getId(),
                aufgabe.getTitel(),
                aufgabe.getBeschreibung(),
                aufgabe.getStatus(),
                BenutzerKurzDto.von(aufgabe.getBearbeiter()),
                aufgabe.getFaelligAm(),
                aufgabe.getErstelltAm()
        );
    }
}
