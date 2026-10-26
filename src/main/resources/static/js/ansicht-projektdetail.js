/*
 * Ansicht "Projektdetail" (#/projekte/{id}).
 *
 * Vereint Stammdaten, Fortschritt, Team und Aufgabenliste eines Projekts. Von
 * hier aus lassen sich Aufgaben anlegen, bearbeiten und im Status umsetzen.
 */
(function () {
    "use strict";

    const PP = window.PP;
    const B = PP.bausteine;
    const h = React.createElement;
    const MUI = window.MaterialUI;

    // ------------------------------------------------------------------
    // Dialog: Aufgabe anlegen / bearbeiten
    // ------------------------------------------------------------------

    function AufgabeDialog(eigenschaften) {
        const bearbeitet = Boolean(eigenschaften.aufgabe);
        const aufgabe = eigenschaften.aufgabe || {};
        const projekt = eigenschaften.projekt;

        const [formular, setFormular] = React.useState({
            titel: aufgabe.titel || "",
            beschreibung: aufgabe.beschreibung || "",
            status: aufgabe.status || "OFFEN",
            bearbeiterId: aufgabe.bearbeiter ? String(aufgabe.bearbeiter.id) : "",
            faelligAm: aufgabe.faelligAm || ""
        });
        const [fehler, setFehler] = React.useState(null);
        const [speichert, setSpeichert] = React.useState(false);

        // Aufgaben koennen nur an Projektbeteiligte vergeben werden. Die
        // Auswahlliste enthaelt daher genau die Projektleitung und die
        // zugeordneten Mitarbeitenden.
        const beteiligte = [projekt.leiter].concat(projekt.mitglieder);

        function aendere(feld, wert) {
            setFormular(alt => Object.assign({}, alt, {[feld]: wert}));
        }

        async function speichern(ereignis) {
            ereignis.preventDefault();
            setSpeichert(true);
            setFehler(null);
            const daten = {
                titel: formular.titel,
                beschreibung: formular.beschreibung === "" ? null : formular.beschreibung,
                status: formular.status,
                bearbeiterId: formular.bearbeiterId === "" ? null : Number(formular.bearbeiterId),
                faelligAm: formular.faelligAm === "" ? null : formular.faelligAm
            };
            try {
                if (bearbeitet) {
                    await PP.api.aufgabeAendern(aufgabe.id, daten);
                } else {
                    await PP.api.aufgabeAnlegen(projekt.id, daten);
                }
                eigenschaften.beiGespeichert();
            } catch (ausnahme) {
                setFehler(ausnahme);
            } finally {
                setSpeichert(false);
            }
        }

        const feldfehler = (fehler && fehler.feldfehler) || {};

        return h(MUI.Dialog, {
            open: true, onClose: eigenschaften.beiSchliessen, fullWidth: true, maxWidth: "sm"
        },
            h("form", {onSubmit: speichern, noValidate: true},
                h(MUI.DialogTitle, null,
                    bearbeitet ? "Aufgabe bearbeiten" : "Neue Aufgabe"),
                h(MUI.DialogContent, {dividers: true},

                    fehler && Object.keys(feldfehler).length === 0
                        ? h(MUI.Alert, {severity: "error", sx: {mb: 2}}, fehler.message)
                        : null,

                    h(MUI.TextField, {
                        label: "Titel", value: formular.titel,
                        onChange: e => aendere("titel", e.target.value),
                        fullWidth: true, required: true, margin: "normal", autoFocus: true,
                        error: Boolean(feldfehler.titel),
                        helperText: feldfehler.titel || " "
                    }),
                    h(MUI.TextField, {
                        label: "Beschreibung", value: formular.beschreibung,
                        onChange: e => aendere("beschreibung", e.target.value),
                        fullWidth: true, multiline: true, minRows: 3, margin: "normal",
                        error: Boolean(feldfehler.beschreibung),
                        helperText: feldfehler.beschreibung || " "
                    }),
                    h(MUI.FormControl, {fullWidth: true, margin: "normal"},
                        h(MUI.InputLabel, {id: "aufgabe-status-label"}, "Status"),
                        h(MUI.Select, {
                            labelId: "aufgabe-status-label", label: "Status",
                            value: formular.status,
                            onChange: e => aendere("status", e.target.value)
                        }, B.AUFGABENSTATUS.map(eintrag =>
                            h(MUI.MenuItem, {key: eintrag.wert, value: eintrag.wert},
                                eintrag.text)))),
                    h(MUI.FormControl, {fullWidth: true, margin: "normal"},
                        h(MUI.InputLabel, {id: "aufgabe-bearbeiter-label"}, "Bearbeitung"),
                        h(MUI.Select, {
                            labelId: "aufgabe-bearbeiter-label", label: "Bearbeitung",
                            value: formular.bearbeiterId,
                            onChange: e => aendere("bearbeiterId", e.target.value)
                        },
                            h(MUI.MenuItem, {value: ""}, "– nicht zugewiesen –"),
                            beteiligte.map(person =>
                                h(MUI.MenuItem, {key: person.id, value: String(person.id)},
                                    person.vollerName)))),
                    h(MUI.TextField, {
                        label: "Fällig am", type: "date", value: formular.faelligAm,
                        onChange: e => aendere("faelligAm", e.target.value),
                        InputLabelProps: {shrink: true}, margin: "normal", fullWidth: true,
                        error: Boolean(feldfehler.faelligAm),
                        helperText: feldfehler.faelligAm || " "
                    })),
                h(MUI.DialogActions, null,
                    h(MUI.Button, {onClick: eigenschaften.beiSchliessen, disabled: speichert},
                        "Abbrechen"),
                    h(MUI.Button, {
                        type: "submit", variant: "contained", disabled: speichert,
                        startIcon: speichert
                            ? h(MUI.CircularProgress, {size: 16, color: "inherit"}) : null
                    }, bearbeitet ? "Änderungen speichern" : "Aufgabe anlegen"))));
    }

    // ------------------------------------------------------------------
    // Aufgabenliste
    // ------------------------------------------------------------------

    /** Eine Zeile der Aufgabentabelle inklusive Statuswechsel direkt in der Liste. */
    function Aufgabenzeile(eigenschaften) {
        const aufgabe = eigenschaften.aufgabe;
        const [wechselt, setWechselt] = React.useState(false);

        async function statusSetzen(ereignis) {
            const neuerStatus = ereignis.target.value;
            setWechselt(true);
            try {
                await PP.api.aufgabeStatusAendern(aufgabe.id, neuerStatus);
                eigenschaften.beiAenderung();
            } catch (ausnahme) {
                eigenschaften.beiFehler(ausnahme);
            } finally {
                setWechselt(false);
            }
        }

        return h(MUI.TableRow, {hover: true},
            h(MUI.TableCell, null,
                h(MUI.Typography, {variant: "body2", fontWeight: 500}, aufgabe.titel),
                aufgabe.beschreibung
                    ? h(MUI.Typography, {variant: "caption", color: "text.secondary",
                        sx: {display: "block"}}, aufgabe.beschreibung)
                    : null),
            h(MUI.TableCell, null,
                aufgabe.bearbeiter
                    ? aufgabe.bearbeiter.vollerName
                    : h(MUI.Typography, {variant: "body2", color: "text.secondary"},
                        "nicht zugewiesen")),
            h(MUI.TableCell, null, B.formatiereDatum(aufgabe.faelligAm)),
            h(MUI.TableCell, null,
                eigenschaften.darfBearbeiten
                    ? h(MUI.Select, {
                        size: "small", value: aufgabe.status, onChange: statusSetzen,
                        disabled: wechselt, sx: {minWidth: 160},
                        "aria-label": "Status der Aufgabe " + aufgabe.titel
                    }, B.AUFGABENSTATUS.map(eintrag =>
                        h(MUI.MenuItem, {key: eintrag.wert, value: eintrag.wert}, eintrag.text)))
                    : h(B.Statuskennzeichen, {status: aufgabe.status})),
            h(MUI.TableCell, {align: "right"},
                eigenschaften.darfBearbeiten
                    ? h(MUI.Box, {sx: {display: "flex", gap: 1, justifyContent: "flex-end"}},
                        h(MUI.Button, {size: "small",
                            onClick: () => eigenschaften.beiBearbeiten(aufgabe)}, "Bearbeiten"),
                        h(MUI.Button, {size: "small", color: "error",
                            onClick: () => eigenschaften.beiLoeschen(aufgabe)}, "Löschen"))
                    : null));
    }

    // ------------------------------------------------------------------
    // Detailansicht
    // ------------------------------------------------------------------

    function Projektdetail(eigenschaften) {
        const projektId = eigenschaften.projektId;
        const profil = eigenschaften.profil;

        const projekt = B.verwendeAnfrage(() => PP.api.projekt(projektId), [projektId]);
        const aufgaben = B.verwendeAnfrage(() => PP.api.aufgaben(projektId), [projektId]);

        const [projektDialog, setProjektDialog] = React.useState(false);
        const [aufgabeDialog, setAufgabeDialog] = React.useState(null);
        const [aktionsfehler, setAktionsfehler] = React.useState(null);
        const [zuLoeschen, setZuLoeschen] = React.useState(null);

        /** Nach jeder Änderung an Aufgaben auch das Projekt neu laden - der
         *  Fortschritt wird serverseitig berechnet. */
        function allesNeuLaden() {
            aufgaben.neuLaden();
            projekt.neuLaden();
        }

        if (projekt.status === "laedt" && !projekt.daten) {
            return h(B.Ladeanzeige, {text: "Projekt wird geladen …"});
        }
        if (projekt.status === "fehler") {
            return h(MUI.Box, null,
                h(MUI.Button, {href: "#/projekte", sx: {mb: 2}}, "Zurück zur Übersicht"),
                h(B.Fehlerhinweis, {
                    fehler: projekt.fehler,
                    titel: projekt.fehler && projekt.fehler.status === 404
                        ? "Projekt nicht verfügbar" : "Projekt konnte nicht geladen werden",
                    beiWiederholung: projekt.neuLaden
                }));
        }
        if (!projekt.daten) {
            return null;
        }

        const daten = projekt.daten;
        const archiviert = daten.status === "ARCHIVIERT";

        // Spiegelt die serverseitige Regel, um Bedienelemente auszublenden, die
        // der Server ohnehin ablehnen wuerde. Verbindlich bleibt der Server.
        const darfVerwalten = profil.rollen.indexOf("ADMIN") >= 0
            || (daten.leiter && daten.leiter.id === profil.benutzerId);
        const istBeteiligt = darfVerwalten
            || daten.mitglieder.some(mitglied => mitglied.id === profil.benutzerId);
        const darfAufgabenBearbeiten = istBeteiligt && !archiviert;

        async function archivierungUmschalten() {
            setAktionsfehler(null);
            try {
                if (archiviert) {
                    await PP.api.projektReaktivieren(daten.id);
                } else {
                    await PP.api.projektArchivieren(daten.id);
                }
                projekt.neuLaden();
            } catch (ausnahme) {
                setAktionsfehler(ausnahme);
            }
        }

        async function aufgabeLoeschen() {
            const aufgabe = zuLoeschen;
            setZuLoeschen(null);
            setAktionsfehler(null);
            try {
                await PP.api.aufgabeLoeschen(aufgabe.id);
                allesNeuLaden();
            } catch (ausnahme) {
                setAktionsfehler(ausnahme);
            }
        }

        const liste = aufgaben.daten || [];

        return h(MUI.Box, null,

            h(MUI.Breadcrumbs, {sx: {mb: 2}, "aria-label": "Navigationspfad"},
                h(MUI.Link, {href: "#/projekte", underline: "hover", color: "inherit"},
                    "Projekte"),
                h(MUI.Typography, {color: "text.primary"}, daten.name)),

            h(MUI.Box, {sx: {display: "flex", flexWrap: "wrap", gap: 2,
                justifyContent: "space-between", alignItems: "flex-start", mb: 3}},
                h(MUI.Box, null,
                    h(MUI.Box, {sx: {display: "flex", alignItems: "center", gap: 1.5,
                        flexWrap: "wrap"}},
                        h(MUI.Typography, {variant: "h5", component: "h1"}, daten.name),
                        h(MUI.Chip, {
                            label: archiviert ? "Archiviert" : "Aktiv",
                            color: archiviert ? "default" : "primary",
                            variant: archiviert ? "outlined" : "filled", size: "small"
                        })),
                    h(MUI.Typography, {variant: "body2", color: "text.secondary", sx: {mt: 0.5}},
                        daten.kunde || "Interner Auftrag")),
                darfVerwalten
                    ? h(MUI.Box, {sx: {display: "flex", gap: 1, flexWrap: "wrap"}},
                        h(MUI.Button, {
                            variant: "outlined", disabled: archiviert,
                            onClick: () => setProjektDialog(true)
                        }, "Projekt bearbeiten"),
                        h(MUI.Button, {
                            variant: "outlined",
                            color: archiviert ? "primary" : "warning",
                            onClick: archivierungUmschalten
                        }, archiviert ? "Reaktivieren" : "Archivieren"))
                    : null),

            archiviert
                ? h(MUI.Alert, {severity: "info", sx: {mb: 3}},
                    "Dieses Projekt ist archiviert. Es kann gelesen, aber nicht "
                    + "verändert werden.")
                : null,

            aktionsfehler
                ? h(B.Fehlerhinweis, {fehler: aktionsfehler, titel: "Aktion nicht möglich"})
                : null,

            h(MUI.Grid, {container: true, spacing: 3, sx: {mb: 1}},
                h(MUI.Grid, {item: true, xs: 12, md: 7},
                    h(B.Abschnitt, {titel: "Projektdaten"},
                        h(MUI.Typography, {variant: "body2", sx: {mb: 2}},
                            daten.beschreibung || "Keine Beschreibung hinterlegt."),
                        h(MUI.Table, {size: "small"},
                            h(MUI.TableBody, null,
                                zeile("Projektleitung",
                                    daten.leiter ? daten.leiter.vollerName : "–"),
                                zeile("Start", B.formatiereDatum(daten.startDatum)),
                                zeile("Ende (geplant)", B.formatiereDatum(daten.endDatum)),
                                zeile("Mandant", profil.mandantName))))),
                h(MUI.Grid, {item: true, xs: 12, md: 5},
                    h(B.Abschnitt, {titel: "Fortschritt"},
                        h(B.Fortschrittsanzeige, {fortschritt: daten.fortschritt})),
                    h(B.Abschnitt, {titel: "Team"},
                        h(MUI.Box, {sx: {display: "flex", flexWrap: "wrap", gap: 1}},
                            h(MUI.Chip, {
                                label: (daten.leiter ? daten.leiter.vollerName : "–")
                                    + " · Leitung",
                                color: "primary", variant: "outlined"
                            }),
                            daten.mitglieder.map(mitglied =>
                                h(MUI.Chip, {key: mitglied.id, label: mitglied.vollerName})),
                            daten.mitglieder.length === 0
                                ? h(MUI.Typography, {variant: "body2", color: "text.secondary"},
                                    "Keine weiteren Mitarbeitenden zugeordnet.")
                                : null)))),

            h(B.Abschnitt, {
                titel: "Aufgaben (" + liste.length + ")",
                aktion: darfAufgabenBearbeiten
                    ? h(MUI.Button, {variant: "contained", size: "small",
                        onClick: () => setAufgabeDialog({neu: true})}, "Aufgabe hinzufügen")
                    : null
            },
                aufgaben.status === "laedt" && !aufgaben.daten
                    ? h(B.Ladeanzeige, {text: "Aufgaben werden geladen …"})
                    : null,
                aufgaben.status === "fehler"
                    ? h(B.Fehlerhinweis, {
                        fehler: aufgaben.fehler,
                        titel: "Aufgaben konnten nicht geladen werden",
                        beiWiederholung: aufgaben.neuLaden
                    })
                    : null,
                aufgaben.status === "erfolg" && liste.length === 0
                    ? h(B.Leerhinweis, {text: "Für dieses Projekt gibt es noch keine Aufgaben."})
                    : null,
                liste.length > 0
                    ? h(MUI.TableContainer, null,
                        h(MUI.Table, {size: "small"},
                            h(MUI.TableHead, null,
                                h(MUI.TableRow, null,
                                    h(MUI.TableCell, null, "Aufgabe"),
                                    h(MUI.TableCell, null, "Bearbeitung"),
                                    h(MUI.TableCell, null, "Fällig am"),
                                    h(MUI.TableCell, null, "Status"),
                                    h(MUI.TableCell, {align: "right"}, "Aktionen"))),
                            h(MUI.TableBody, null,
                                liste.map(aufgabe =>
                                    h(Aufgabenzeile, {
                                        key: aufgabe.id,
                                        aufgabe: aufgabe,
                                        darfBearbeiten: darfAufgabenBearbeiten,
                                        beiAenderung: allesNeuLaden,
                                        beiFehler: setAktionsfehler,
                                        beiBearbeiten: gewaehlt =>
                                            setAufgabeDialog({aufgabe: gewaehlt}),
                                        beiLoeschen: setZuLoeschen
                                    })))))
                    : null),

            projektDialog
                ? h(PP.ProjektDialog, {
                    projekt: daten,
                    beiSchliessen: () => setProjektDialog(false),
                    beiGespeichert: () => {
                        setProjektDialog(false);
                        allesNeuLaden();
                    }
                })
                : null,

            aufgabeDialog
                ? h(AufgabeDialog, {
                    projekt: daten,
                    aufgabe: aufgabeDialog.aufgabe || null,
                    beiSchliessen: () => setAufgabeDialog(null),
                    beiGespeichert: () => {
                        setAufgabeDialog(null);
                        allesNeuLaden();
                    }
                })
                : null,

            zuLoeschen
                ? h(MUI.Dialog, {open: true, onClose: () => setZuLoeschen(null)},
                    h(MUI.DialogTitle, null, "Aufgabe löschen?"),
                    h(MUI.DialogContent, null,
                        h(MUI.Typography, null,
                            "Die Aufgabe „" + zuLoeschen.titel
                            + "“ wird dauerhaft entfernt.")),
                    h(MUI.DialogActions, null,
                        h(MUI.Button, {onClick: () => setZuLoeschen(null)}, "Abbrechen"),
                        h(MUI.Button, {color: "error", variant: "contained",
                            onClick: aufgabeLoeschen}, "Löschen")))
                : null);
    }

    /** Kleine Hilfsfunktion fuer zweispaltige Datenzeilen. */
    function zeile(bezeichnung, wert) {
        return h(MUI.TableRow, {key: bezeichnung},
            h(MUI.TableCell, {sx: {pl: 0, border: 0, color: "text.secondary",
                whiteSpace: "nowrap", width: 180}}, bezeichnung),
            h(MUI.TableCell, {sx: {border: 0}}, wert));
    }

    PP.Projektdetail = Projektdetail;
})();
