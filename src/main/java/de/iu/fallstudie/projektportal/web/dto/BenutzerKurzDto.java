package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Benutzer;

/**
 * Minimaldarstellung eines Benutzers fuer Referenzen in Projekten und Aufgaben.
 * Enthaelt bewusst keine sicherheitsrelevanten Felder.
 */
public record BenutzerKurzDto(Long id, String vollerName, String email) {

    public static BenutzerKurzDto von(Benutzer benutzer) {
        if (benutzer == null) {
            return null;
        }
        return new BenutzerKurzDto(benutzer.getId(), benutzer.getVollerName(), benutzer.getEmail());
    }
}
