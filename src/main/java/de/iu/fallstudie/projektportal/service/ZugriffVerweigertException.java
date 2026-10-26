package de.iu.fallstudie.projektportal.service;

/**
 * Der Benutzer ist angemeldet, darf die angeforderte Aktion aber nicht
 * ausfuehren. Wird auf HTTP 403 (Forbidden) abgebildet.
 *
 * <p>Diese Exception deckt die feingranulare Autorisierung auf Datensatzebene
 * ab (z. B. "nur Mitglieder eines Projekts duerfen dessen Aufgaben bearbeiten").
 * Die grobgranulare, rollenbasierte Pruefung erfolgt vorgelagert deklarativ
 * ueber Spring Security.</p>
 */
public class ZugriffVerweigertException extends RuntimeException {

    public ZugriffVerweigertException(String meldung) {
        super(meldung);
    }
}
