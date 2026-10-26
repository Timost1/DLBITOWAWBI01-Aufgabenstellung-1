package de.iu.fallstudie.projektportal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mandant (Tenant) als Wurzel der Datenhoheit.
 *
 * <p>Die Anwendung ist mandantenfaehig vorbereitet: Jeder {@link Benutzer} und
 * jedes {@link Projekt} gehoert genau einem Mandanten. Aufgaben erben ihren
 * Mandanten transitiv ueber das Projekt. Alle lesenden und schreibenden Zugriffe
 * der Serviceschicht sind auf den Mandanten des angemeldeten Benutzers
 * eingeschraenkt (Shared-Database-/Shared-Schema-Ansatz mit Diskriminatorspalte).</p>
 */
@Entity
@Table(name = "mandant")
public class Mandant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String kuerzel;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean aktiv = true;

    protected Mandant() {
        // von JPA benoetigter Standardkonstruktor
    }

    public Mandant(String kuerzel, String name) {
        this.kuerzel = kuerzel;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getKuerzel() {
        return kuerzel;
    }

    public void setKuerzel(String kuerzel) {
        this.kuerzel = kuerzel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }
}
