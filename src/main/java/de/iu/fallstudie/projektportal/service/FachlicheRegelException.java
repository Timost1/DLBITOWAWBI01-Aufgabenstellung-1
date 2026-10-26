package de.iu.fallstudie.projektportal.service;

/**
 * Verstoss gegen eine fachliche Regel, die Bean Validation nicht abdecken kann,
 * weil sie mehrere Felder oder den Datenbestand einbezieht.
 *
 * <p>Beispiele: doppelter Projektname innerhalb eines Mandanten, Enddatum vor
 * Startdatum, Schreibzugriff auf ein archiviertes Projekt. Die Exception wird
 * vom globalen Fehler-Handler auf HTTP 409 (Conflict) abgebildet.</p>
 */
public class FachlicheRegelException extends RuntimeException {

    public FachlicheRegelException(String meldung) {
        super(meldung);
    }
}
