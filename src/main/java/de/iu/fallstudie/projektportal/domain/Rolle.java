package de.iu.fallstudie.projektportal.domain;

/**
 * Rollen der rollenbasierten Zugriffskontrolle (RBAC).
 *
 * <p>Die Namen entsprechen den Spring-Security-Autoritaeten ohne das Praefix
 * {@code ROLE_}; dieses ergaenzt {@code AngemeldeterBenutzer} beim Aufbau der
 * {@code GrantedAuthority}-Liste, damit {@code hasRole('ADMIN')} greift.</p>
 */
public enum Rolle {

    /** Verwaltet Benutzerkonten und Rollen des eigenen Mandanten. */
    ADMIN,

    /** Legt Projekte an, ordnet Mitarbeitende zu und sieht den Projektfortschritt. */
    PROJEKTLEITER,

    /** Arbeitet an Aufgaben der Projekte, denen er oder sie zugeordnet ist. */
    MITARBEITER
}
