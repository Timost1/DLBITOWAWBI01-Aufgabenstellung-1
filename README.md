# Projektportal

Projekt- und Aufgabenmanagementsystem für ein IT-Dienstleistungsunternehmen.

Fallstudie zum Kurs **DLBITOWAWBI01 – Programmierung von Web-Anwendungen:
webbasierte betriebliche Informationssysteme** (IU Internationale Hochschule),
Aufgabenstellung 1.

---

## Technologiestack

Der Stack folgt dem Lernskript des Kurses; es werden keine darüber hinausgehenden
Frameworks eingesetzt.

| Schicht | Technologie |
|---|---|
| Build und Abhängigkeiten | Maven (`pom.xml`) |
| Laufzeit | Java 21, Spring Boot 3.5 mit eingebettetem Tomcat |
| Web-/API-Schicht | Spring MVC (`spring-boot-starter-web`), REST über JSON |
| Eingabeprüfung | Bean Validation / Hibernate Validator (`spring-boot-starter-validation`) |
| Sicherheit | Spring Security (HTTP Basic, Sitzung, Rollen, CSRF) |
| Persistenz | Spring Data JPA mit Hibernate als JPA-Provider |
| Datenbank | H2 (relational, Dateimodus – die Daten bleiben erhalten) |
| Frontend | React 18 als UMD-Build, Material UI 5, Hooks (`useState`, `useEffect`, `useReducer`) |
| Tests | JUnit 5, AssertJ, MockMvc, Spring Security Test |

Das Frontend wird bewusst ohne Build-Werkzeug betrieben: React, ReactDOM und
Material UI liegen als UMD-Bibliotheken unter `src/main/resources/static/vendor`
und werden von Spring Boot als statische Dateien ausgeliefert. Die Oberfläche ist
mit `React.createElement` aufgebaut – so, wie es das Lernskript einführt.

## Anwendung starten

Voraussetzungen: JDK 21 oder neuer, Maven 3.9 oder neuer.

```bash
mvn spring-boot:run
```

Anschließend ist die Anwendung unter <http://localhost:8080> erreichbar. Beim
ersten Start legt die Anwendung einen Demodatenbestand an (zwei Mandanten, acht
Benutzerkonten, vier Projekte, 14 Aufgaben). Die Datenbankdateien entstehen im
Ordner `daten/`.

### Zugänge des Demodatenbestands

Kennwort für alle Demokonten: `Fallstudie2026!`

| Rolle | Anmeldename |
|---|---|
| Administrator:in | `admin@nordlicht.example` |
| Projektleiter:in | `anna.behrens@nordlicht.example` |
| Projektleiter:in | `tobias.marek@nordlicht.example` |
| Mitarbeiter:in | `lea.novak@nordlicht.example` |
| Mitarbeiter:in | `jonas.riedl@nordlicht.example` |
| Mitarbeiter:in | `mira.kaya@nordlicht.example` |

Der zweite Mandant (`admin@orbit.example`, `ruth.olsen@orbit.example`) dient dazu,
die Mandantentrennung nachvollziehbar zu machen: Seine Projekte sind für Konten
des Mandanten *Nordlicht* weder sichtbar noch über die API erreichbar.

## Tests ausführen

```bash
mvn test
```

58 Tests in fünf Klassen: Fachlogik (Sichtbarkeit, Mandantentrennung,
Fortschritt, Archivierung, Rollenschutz), HTTP-Vertrag (Statuscodes,
Fehlerformat, Authentifizierung, Autorisierung, CSRF) und ein reiner Modultest
der Fortschrittsberechnung.

## Projektstruktur

```
src/main/java/de/iu/fallstudie/projektportal/
├─ ProjektportalApplication.java   Einstiegspunkt
├─ config/                         Security, CSRF-Filter, Demodaten
├─ domain/                         JPA-Entities und Enumerationen
├─ repository/                     Spring-Data-JPA-Repositories
├─ security/                       UserDetails-Anbindung an die Datenbank
├─ service/                        Fachlogik und Zugriffskontrolle
└─ web/                            REST-Controller, DTOs, Fehlerbehandlung

src/main/resources/
├─ application.properties
└─ static/                         Single-Page-Anwendung
   ├─ index.html, style.css
   ├─ js/                          api, bausteine, vier Ansichten, app
   └─ vendor/                      React, ReactDOM, Material UI (UMD)

src/test/java/…                    Integrations- und Modultests
docs/
├─ abbildungen/                    Diagramme (SVG und PNG) und Screenshots
└─ datenbankschema.sql             von Hibernate erzeugtes Schema
```

## REST-Schnittstelle

| Methode | Pfad | Zweck | Berechtigung |
|---|---|---|---|
| `GET` | `/api/csrf` | CSRF-Token als Cookie ausgeben | offen |
| `POST` | `/api/auth/login` | Anmelden (HTTP Basic) | gültige Anmeldedaten |
| `GET` | `/api/auth/sitzung` | Profil der laufenden Sitzung | angemeldet |
| `POST` | `/api/auth/logout` | Abmelden, Sitzung verwerfen | angemeldet |
| `GET` | `/api/projekte` | berechtigte Projekte auflisten | angemeldet |
| `GET` | `/api/projekte/{id}` | Projektdetails | am Projekt beteiligt |
| `POST` | `/api/projekte` | Projekt anlegen | `PROJEKTLEITER`, `ADMIN` |
| `PUT` | `/api/projekte/{id}` | Projektdaten ersetzen | verantwortliche Leitung, `ADMIN` |
| `POST` | `/api/projekte/{id}/archivierung` | archivieren | verantwortliche Leitung, `ADMIN` |
| `POST` | `/api/projekte/{id}/reaktivierung` | Archivierung zurücknehmen | verantwortliche Leitung, `ADMIN` |
| `GET` | `/api/projekte/{id}/aufgaben` | Aufgaben eines Projekts | am Projekt beteiligt |
| `POST` | `/api/projekte/{id}/aufgaben` | Aufgabe anlegen | am Projekt beteiligt |
| `PUT` | `/api/aufgaben/{id}` | Aufgabe bearbeiten | am Projekt beteiligt |
| `PATCH` | `/api/aufgaben/{id}/status` | Status setzen | am Projekt beteiligt |
| `DELETE` | `/api/aufgaben/{id}` | Aufgabe löschen | am Projekt beteiligt |
| `GET` | `/api/mitarbeitende` | Auswahlliste aktiver Konten | angemeldet |
| `GET` | `/api/benutzer` | Konten des Mandanten | `ADMIN` |
| `POST` | `/api/benutzer` | Konto anlegen, Rollen vergeben | `ADMIN` |
| `PUT` | `/api/benutzer/{id}` | Konto ändern | `ADMIN` |

Fehler werden einheitlich als JSON zurückgegeben:

```json
{
  "zeitpunkt": "2026-09-12T13:04:52.394Z",
  "status": 400,
  "fehlercode": "VALIDIERUNG",
  "meldung": "Die Eingaben sind unvollständig oder ungültig.",
  "feldfehler": { "name": "Der Projektname ist ein Pflichtfeld." }
}
```

## Sicherheitsmechanismen

- **Authentifizierung:** HTTP Basic beim Anmelden; danach trägt eine
  serverseitige Sitzung die Authentifizierung. Das Kennwort wird genau einmal
  übertragen und nicht im Browser vorgehalten.
- **Abmeldung:** `POST /api/auth/logout` invalidiert die Sitzung serverseitig.
- **Autorisierung:** grobgranular über Pfadregeln und `@PreAuthorize`,
  feingranular auf Datensatzebene im `ZugriffskontrollService`.
- **Mandantentrennung:** Der Mandant wird ausschließlich dem Sicherheitskontext
  entnommen; jede Repository-Abfrage führt ihn als Parameter mit.
- **CSRF:** Token im Cookie `XSRF-TOKEN`, Rücksendung im Header `X-XSRF-TOKEN`.
- **Kennwörter:** BCrypt-Hashes; der Hash verlässt den Server nie.
- **Eingabeprüfung:** Bean Validation auf allen Eingabe-DTOs, ergänzt um
  fachliche Regeln in der Serviceschicht.
- **Fehlerausgabe:** nur fachliche Meldungen; Stacktraces bleiben im Serverlog.

## Hinweise zum Betrieb

Für den produktiven Einsatz wären zusätzlich erforderlich: HTTPS mit
`same-site`-Cookies, ein Datenbankserver anstelle der eingebetteten H2-Datenbank,
versionierte Schemamigrationen (etwa Flyway) statt `ddl-auto=update` sowie
Passwortrichtlinien und eine Sperre nach fehlgeschlagenen Anmeldeversuchen.
