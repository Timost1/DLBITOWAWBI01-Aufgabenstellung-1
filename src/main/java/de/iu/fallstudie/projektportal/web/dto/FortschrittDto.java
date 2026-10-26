package de.iu.fallstudie.projektportal.web.dto;

/**
 * Projektfortschritt, abgeleitet aus dem Anteil erledigter Aufgaben.
 *
 * <p>Die Kennzahl wird serverseitig berechnet und nicht im Client, damit alle
 * Clients dieselbe Definition verwenden (User Story "Fortschritt eines Projekts
 * anhand erledigter Aufgaben sehen").</p>
 */
public record FortschrittDto(
        long anzahlGesamt,
        long anzahlOffen,
        long anzahlInBearbeitung,
        long anzahlErledigt,
        int prozentErledigt
) {

    public static FortschrittDto berechnen(long gesamt, long offen, long inBearbeitung, long erledigt) {
        int prozent = gesamt == 0 ? 0 : (int) Math.round(erledigt * 100.0 / gesamt);
        return new FortschrittDto(gesamt, offen, inBearbeitung, erledigt, prozent);
    }
}
