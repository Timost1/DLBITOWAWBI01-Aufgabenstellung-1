package de.iu.fallstudie.projektportal.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Kundenprojekt eines Mandanten.
 *
 * <p>Ein Projekt hat genau eine verantwortliche Projektleitung und beliebig
 * viele zugeordnete Mitarbeitende ({@code n:m}-Beziehung ueber die Tabelle
 * {@code projekt_mitglied}). Die Zuordnung entscheidet zugleich darueber, wer
 * das Projekt sehen darf.</p>
 */
@Entity
@Table(
        name = "projekt",
        uniqueConstraints = @UniqueConstraint(name = "uk_projekt_mandant_name",
                columnNames = {"mandant_id", "name"})
)
public class Projekt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "mandant_id", nullable = false)
    private Mandant mandant;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String kunde;

    @Column(length = 2000)
    private String beschreibung;

    /**
     * Der Status wird als Zeichenkette gespeichert. Die explizite
     * {@code columnDefinition} verhindert, dass Hibernate den
     * datenbankspezifischen ENUM-Typ von H2 verwendet; das Schema bleibt so
     * standardkonform und beim Wechsel des Datenbanksystems unveraendert.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Projektstatus status = Projektstatus.AKTIV;

    @Column(name = "start_datum")
    private LocalDate startDatum;

    @Column(name = "end_datum")
    private LocalDate endDatum;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "leiter_id", nullable = false)
    private Benutzer leiter;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "projekt_mitglied",
            joinColumns = @JoinColumn(name = "projekt_id"),
            inverseJoinColumns = @JoinColumn(name = "benutzer_id")
    )
    private Set<Benutzer> mitglieder = new LinkedHashSet<>();

    /**
     * Aufgaben des Projekts. Die Beziehung dient der Modellierung des Aggregats:
     * Mit dem Projekt werden auch seine Aufgaben entfernt. Die Fremdschluessel
     * pflegt die besitzende Seite {@link Aufgabe#getProjekt()}, weshalb die
     * Liste im Anwendungscode nicht manuell fortgeschrieben wird.
     */
    @OneToMany(mappedBy = "projekt", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<Aufgabe> aufgaben = new ArrayList<>();

    protected Projekt() {
        // von JPA benoetigter Standardkonstruktor
    }

    public Projekt(Mandant mandant, String name, Benutzer leiter) {
        this.mandant = mandant;
        this.name = name;
        this.leiter = leiter;
    }

    /**
     * Prueft, ob ein Benutzer fachlich Zugriff auf dieses Projekt hat. Ein
     * Administrator ist hier bewusst nicht beruecksichtigt; diese Ausnahme
     * entscheidet die Serviceschicht.
     */
    public boolean istBeteiligt(Benutzer benutzer) {
        if (benutzer == null || benutzer.getId() == null) {
            return false;
        }
        if (benutzer.getId().equals(leiter.getId())) {
            return true;
        }
        return mitglieder.stream()
                .anyMatch(mitglied -> benutzer.getId().equals(mitglied.getId()));
    }

    public boolean istArchiviert() {
        return status == Projektstatus.ARCHIVIERT;
    }

    public void archivieren() {
        this.status = Projektstatus.ARCHIVIERT;
    }

    public void reaktivieren() {
        this.status = Projektstatus.AKTIV;
    }

    public Long getId() {
        return id;
    }

    public Mandant getMandant() {
        return mandant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKunde() {
        return kunde;
    }

    public void setKunde(String kunde) {
        this.kunde = kunde;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public Projektstatus getStatus() {
        return status;
    }

    public void setStatus(Projektstatus status) {
        this.status = status;
    }

    public LocalDate getStartDatum() {
        return startDatum;
    }

    public void setStartDatum(LocalDate startDatum) {
        this.startDatum = startDatum;
    }

    public LocalDate getEndDatum() {
        return endDatum;
    }

    public void setEndDatum(LocalDate endDatum) {
        this.endDatum = endDatum;
    }

    public Benutzer getLeiter() {
        return leiter;
    }

    public void setLeiter(Benutzer leiter) {
        this.leiter = leiter;
    }

    public Set<Benutzer> getMitglieder() {
        return mitglieder;
    }

    /** Ersetzt die Mitgliederliste in place, damit Hibernate die Aenderung nachvollzieht. */
    public void setMitglieder(Set<Benutzer> neueMitglieder) {
        this.mitglieder.clear();
        this.mitglieder.addAll(neueMitglieder);
    }

    public List<Aufgabe> getAufgaben() {
        return aufgaben;
    }
}
