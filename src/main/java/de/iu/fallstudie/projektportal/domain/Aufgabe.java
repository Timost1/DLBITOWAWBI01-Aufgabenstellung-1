package de.iu.fallstudie.projektportal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Aufgabe innerhalb eines Projekts.
 *
 * <p>Die Aufgabe ist Teil des Aggregats {@link Projekt} und traegt keinen
 * eigenen Mandantenbezug; dieser ergibt sich transitiv aus dem Projekt. Damit
 * bleibt die Mandantentrennung an einer einzigen Stelle konsistent.</p>
 */
@Entity
@Table(name = "aufgabe")
public class Aufgabe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "projekt_id", nullable = false)
    private Projekt projekt;

    @Column(nullable = false, length = 160)
    private String titel;

    @Column(length = 2000)
    private String beschreibung;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Aufgabenstatus status = Aufgabenstatus.OFFEN;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bearbeiter_id")
    private Benutzer bearbeiter;

    @Column(name = "faellig_am")
    private LocalDate faelligAm;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private Instant erstelltAm;

    protected Aufgabe() {
        // von JPA benoetigter Standardkonstruktor
    }

    public Aufgabe(Projekt projekt, String titel) {
        this.projekt = projekt;
        this.titel = titel;
    }

    @PrePersist
    void vorDemSpeichern() {
        if (erstelltAm == null) {
            erstelltAm = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Projekt getProjekt() {
        return projekt;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public Aufgabenstatus getStatus() {
        return status;
    }

    public void setStatus(Aufgabenstatus status) {
        this.status = status;
    }

    public Benutzer getBearbeiter() {
        return bearbeiter;
    }

    public void setBearbeiter(Benutzer bearbeiter) {
        this.bearbeiter = bearbeiter;
    }

    public LocalDate getFaelligAm() {
        return faelligAm;
    }

    public void setFaelligAm(LocalDate faelligAm) {
        this.faelligAm = faelligAm;
    }

    public Instant getErstelltAm() {
        return erstelltAm;
    }
}
