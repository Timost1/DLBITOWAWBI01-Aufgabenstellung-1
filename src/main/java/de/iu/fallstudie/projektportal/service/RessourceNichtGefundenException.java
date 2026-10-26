package de.iu.fallstudie.projektportal.service;

/**
 * Die angeforderte Ressource existiert nicht oder liegt ausserhalb des
 * Mandanten des angemeldeten Benutzers.
 *
 * <p>Beide Faelle werden bewusst gleich behandelt und mit HTTP 404 beantwortet.
 * Eine Unterscheidung ("existiert, aber nicht fuer Dich") wuerde Rueckschluesse
 * auf fremde Datenbestaende zulassen.</p>
 */
public class RessourceNichtGefundenException extends RuntimeException {

    public RessourceNichtGefundenException(String meldung) {
        super(meldung);
    }
}
