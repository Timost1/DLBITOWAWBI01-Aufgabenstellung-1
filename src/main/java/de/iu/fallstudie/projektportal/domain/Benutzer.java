package de.iu.fallstudie.projektportal.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.EnumSet;
import java.util.Set;

/**
 * Benutzerkonto eines Mandanten.
 *
 * <p>Das Kennwort wird ausschliesslich als BCrypt-Hash gespeichert. Die
 * E-Mail-Adresse dient als Anmeldename und ist pro Mandant eindeutig.</p>
 */
@Entity
@Table(
        name = "benutzer",
        uniqueConstraints = @UniqueConstraint(name = "uk_benutzer_mandant_email",
                columnNames = {"mandant_id", "email"})
)
public class Benutzer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "mandant_id", nullable = false)
    private Mandant mandant;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "passwort_hash", nullable = false, length = 100)
    private String passwortHash;

    @Column(nullable = false, length = 60)
    private String vorname;

    @Column(nullable = false, length = 60)
    private String nachname;

    @Column(nullable = false)
    private boolean aktiv = true;

    /**
     * Rollen des Benutzers. Als {@code ElementCollection} abgebildet, damit ein
     * Konto mehrere Rollen tragen kann (z. B. Projektleitung und Mitarbeit).
     */
    @ElementCollection(fetch = FetchType.EAGER, targetClass = Rolle.class)
    @CollectionTable(name = "benutzer_rolle", joinColumns = @JoinColumn(name = "benutzer_id"))
    @Column(name = "rolle", nullable = false, length = 30, columnDefinition = "varchar(30)")
    @Enumerated(EnumType.STRING)
    private Set<Rolle> rollen = EnumSet.noneOf(Rolle.class);

    protected Benutzer() {
        // von JPA benoetigter Standardkonstruktor
    }

    public Benutzer(Mandant mandant, String email, String passwortHash,
                    String vorname, String nachname, Set<Rolle> rollen) {
        this.mandant = mandant;
        this.email = email;
        this.passwortHash = passwortHash;
        this.vorname = vorname;
        this.nachname = nachname;
        this.rollen.addAll(rollen);
    }

    /** Vollstaendiger Name fuer Anzeigen in der Oberflaeche. */
    public String getVollerName() {
        return vorname + " " + nachname;
    }

    public boolean hatRolle(Rolle rolle) {
        return rollen.contains(rolle);
    }

    public Long getId() {
        return id;
    }

    public Mandant getMandant() {
        return mandant;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswortHash() {
        return passwortHash;
    }

    public void setPasswortHash(String passwortHash) {
        this.passwortHash = passwortHash;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Set<Rolle> getRollen() {
        return rollen;
    }

    /**
     * Ersetzt die Rollenzuordnung. Die verwaltete Collection wird bewusst in
     * place veraendert, damit Hibernate die Aenderung am selben Collection-Wrapper
     * erkennt und nicht als Dereferenzierung behandelt.
     */
    public void setRollen(Set<Rolle> neueRollen) {
        this.rollen.clear();
        this.rollen.addAll(neueRollen);
    }
}
