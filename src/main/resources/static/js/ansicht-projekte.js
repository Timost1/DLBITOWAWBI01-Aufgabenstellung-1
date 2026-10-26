/*
 * Ansicht "Projektübersicht" (#/projekte) sowie der Dialog zum Anlegen und
 * Bearbeiten eines Projekts. Der Dialog wird auch von der Detailansicht
 * verwendet und liegt deshalb hier als eigenständige Komponente.
 */
(function () {
    "use strict";

    const PP = window.PP;
    const B = PP.bausteine;
    const h = React.createElement;
    const MUI = window.MaterialUI;

    // ------------------------------------------------------------------
    // Dialog: Projekt anlegen / bearbeiten
    // ------------------------------------------------------------------

    /**
     * Formulardialog fuer die Projektstammdaten.
     *
     * @param eigenschaften.projekt      vorhandenes Projekt (oder null zum Anlegen)
     * @param eigenschaften.beiSchliessen Rueckmeldung ohne Speichern
     * @param eigenschaften.beiGespeichert Rueckmeldung nach erfolgreichem Speichern
     */
    function ProjektDialog(eigenschaften) {
        const bearbeitet = Boolean(eigenschaften.projekt);
        const projekt = eigenschaften.projekt || {};

        const [formular, setFormular] = React.useState({
            name: projekt.name || "",
            kunde: projekt.kunde || "",
            beschreibung: projekt.beschreibung || "",
            leiterId: projekt.leiter ? String(projekt.leiter.id) : "",
            startDatum: projekt.startDatum || "",
            endDatum: projekt.endDatum || "",
            mitgliedIds: (projekt.mitglieder || []).map(mitglied => mitglied.id)
        });
        const [fehler, setFehler] = React.useState(null);
        const [speichert, setSpeichert] = React.useState(false);

        const mitarbeitende = B.verwendeAnfrage(() => PP.api.mitarbeitende(), []);

        function aendere(feld, wert) {
            setFormular(alt => Object.assign({}, alt, {[feld]: wert}));
        }

        /** Leere Zeichenketten werden zu null, damit der Server sie als "nicht gesetzt" liest. */
        function leerZuNull(wert) {
            return wert === "" ? null : wert;
        }

        async function speichern(ereignis) {
            ereignis.preventDefault();
            setSpeichert(true);
            setFehler(null);
            const daten = {
                name: formular.name,
                kunde: leerZuNull(formular.kunde),
                beschreibung: leerZuNull(formular.beschreibung),
                leiterId: formular.leiterId === "" ? null : Number(formular.leiterId),
                startDatum: leerZuNull(formular.startDatum),
                endDatum: leerZuNull(formular.endDatum),
                mitgliedIds: formular.mitgliedIds
            };
            try {
                const gespeichert = bearbeitet
                    ? await PP.api.projektAendern(projekt.id, daten)
                    : await PP.api.projektAnlegen(daten);
                eigenschaften.beiGespeichert(gespeichert);
            } catch (ausnahme) {
                setFehler(ausnahme);
            } finally {
                setSpeichert(false);
            }
        }

        const feldfehler = (fehler && fehler.feldfehler) || {};
        const auswahl = mitarbeitende.daten || [];

        return h(MUI.Dialog, {
            open: true,
            onClose: eigenschaften.beiSchliessen,
            fullWidth: true,
            maxWidth: "sm",
            scroll: "paper"
        },
            h("form", {onSubmit: speichern, noValidate: true},
                h(MUI.DialogTitle, null,
                    bearbeitet ? "Projekt bearbeiten" : "Neues Projekt anlegen"),

                h(MUI.DialogContent, {dividers: true},

                    fehler && Object.keys(feldfehler).length === 0
                        ? h(MUI.Alert, {severity: "error", sx: {mb: 2}}, fehler.message)
                        : null,

                    h(MUI.TextField, {
                        label: "Projektname",
                        value: formular.name,
                        onChange: e => aendere("name", e.target.value),
                        fullWidth: true, required: true, margin: "normal",
                        error: Boolean(feldfehler.name),
                        helperText: feldfehler.name || " "
                    }),
                    h(MUI.TextField, {
                        label: "Kunde",
                        value: formular.kunde,
                        onChange: e => aendere("kunde", e.target.value),
                        fullWidth: true, margin: "normal",
                        error: Boolean(feldfehler.kunde),
                        helperText: feldfehler.kunde || " "
                    }),
                    h(MUI.TextField, {
                        label: "Beschreibung",
                        value: formular.beschreibung,
                        onChange: e => aendere("beschreibung", e.target.value),
                        fullWidth: true, multiline: true, minRows: 3, margin: "normal",
                        error: Boolean(feldfehler.beschreibung),
                        helperText: feldfehler.beschreibung || " "
                    }),

                    mitarbeitende.status === "fehler"
                        ? h(B.Fehlerhinweis, {
                            fehler: mitarbeitende.fehler,
                            titel: "Mitarbeitende konnten nicht geladen werden",
                            beiWiederholung: mitarbeitende.neuLaden
                        })
                        : null,

                    h(MUI.FormControl, {
                        fullWidth: true, margin: "normal", required: true,
                        error: Boolean(feldfehler.leiterId)
                    },
                        h(MUI.InputLabel, {id: "leitung-label"}, "Projektleitung"),
                        h(MUI.Select, {
                            labelId: "leitung-label",
                            label: "Projektleitung",
                            value: formular.leiterId,
                            onChange: e => aendere("leiterId", e.target.value),
                            disabled: mitarbeitende.status !== "erfolg"
                        }, auswahl.map(person =>
                            h(MUI.MenuItem, {key: person.id, value: String(person.id)},
                                person.vollerName))),
                        h(MUI.FormHelperText, null,
                            feldfehler.leiterId
                            || "Nur Konten mit der Rolle Projektleiter:in sind zulässig.")),

                    h(MUI.FormControl, {fullWidth: true, margin: "normal"},
                        h(MUI.InputLabel, {id: "mitglieder-label"}, "Zugeordnete Mitarbeitende"),
                        h(MUI.Select, {
                            labelId: "mitglieder-label",
                            label: "Zugeordnete Mitarbeitende",
                            multiple: true,
                            value: formular.mitgliedIds,
                            onChange: e => aendere("mitgliedIds", e.target.value),
                            disabled: mitarbeitende.status !== "erfolg",
                            renderValue: gewaehlt => auswahl
                                .filter(person => gewaehlt.indexOf(person.id) >= 0)
                                .map(person => person.vollerName)
                                .join(", ")
                        }, auswahl.map(person =>
                            h(MUI.MenuItem, {key: person.id, value: person.id},
                                h(MUI.Checkbox, {
                                    checked: formular.mitgliedIds.indexOf(person.id) >= 0
                                }),
                                h(MUI.ListItemText, {primary: person.vollerName,
                                    secondary: person.email})))),
                        h(MUI.FormHelperText, null,
                            "Nur zugeordnete Mitarbeitende sehen das Projekt und "
                            + "können seine Aufgaben bearbeiten.")),

                    h(MUI.Box, {sx: {display: "flex", gap: 2, mt: 1, flexWrap: "wrap"}},
                        h(MUI.TextField, {
                            label: "Startdatum", type: "date",
                            value: formular.startDatum,
                            onChange: e => aendere("startDatum", e.target.value),
                            InputLabelProps: {shrink: true}, sx: {flex: 1, minWidth: 160},
                            error: Boolean(feldfehler.startDatum),
                            helperText: feldfehler.startDatum || " "
                        }),
                        h(MUI.TextField, {
                            label: "Enddatum (geplant)", type: "date",
                            value: formular.endDatum,
                            onChange: e => aendere("endDatum", e.target.value),
                            InputLabelProps: {shrink: true}, sx: {flex: 1, minWidth: 160},
                            error: Boolean(feldfehler.endDatum),
                            helperText: feldfehler.endDatum || " "
                        }))),

                h(MUI.DialogActions, null,
                    h(MUI.Button, {onClick: eigenschaften.beiSchliessen, disabled: speichert},
                        "Abbrechen"),
                    h(MUI.Button, {
                        type: "submit", variant: "contained", disabled: speichert,
                        startIcon: speichert
                            ? h(MUI.CircularProgress, {size: 16, color: "inherit"}) : null
                    }, bearbeitet ? "Änderungen speichern" : "Projekt anlegen"))));
    }

    // ------------------------------------------------------------------
    // Übersicht
    // ------------------------------------------------------------------

    /** Kachel eines Projekts in der Übersicht. */
    function Projektkachel(eigenschaften) {
        const projekt = eigenschaften.projekt;
        const archiviert = projekt.status === "ARCHIVIERT";

        return h(MUI.Card, {variant: "outlined", sx: {height: "100%", display: "flex",
            flexDirection: "column"}},
            h(MUI.CardContent, {sx: {flexGrow: 1}},
                h(MUI.Box, {sx: {display: "flex", justifyContent: "space-between",
                    alignItems: "flex-start", gap: 1, mb: 1}},
                    h(MUI.Typography, {variant: "h6", component: "h3", sx: {lineHeight: 1.3}},
                        projekt.name),
                    h(MUI.Chip, {
                        label: archiviert ? "Archiviert" : "Aktiv",
                        color: archiviert ? "default" : "primary",
                        variant: archiviert ? "outlined" : "filled",
                        size: "small"
                    })),
                h(MUI.Typography, {variant: "body2", color: "text.secondary", sx: {mb: 1.5}},
                    projekt.kunde || "Interner Auftrag"),
                h(MUI.Typography, {variant: "body2", sx: {mb: 2}},
                    "Leitung: " + (projekt.leiter ? projekt.leiter.vollerName : "–")
                    + " · Team: " + (projekt.mitglieder.length + 1)),
                h(B.Fortschrittsanzeige, {fortschritt: projekt.fortschritt}),
                h(MUI.Typography, {variant: "caption", color: "text.secondary",
                    sx: {display: "block", mt: 1.5}},
                    "Zeitraum: " + B.formatiereDatum(projekt.startDatum)
                    + " – " + B.formatiereDatum(projekt.endDatum))),
            h(MUI.CardActions, {sx: {px: 2, pb: 2}},
                h(MUI.Button, {
                    size: "small", variant: "outlined",
                    href: "#/projekte/" + projekt.id
                }, "Projekt öffnen")));
    }

    /**
     * Einstiegsansicht nach der Anmeldung. Zeigt ausschliesslich die Projekte,
     * fuer die der angemeldete Benutzer berechtigt ist - die Auswahl trifft
     * bereits der Server.
     */
    function Projektuebersicht(eigenschaften) {
        const profil = eigenschaften.profil;
        const projekte = B.verwendeAnfrage(() => PP.api.projekte(), []);
        const [filter, setFilter] = React.useState("AKTIV");
        const [dialogOffen, setDialogOffen] = React.useState(false);

        const darfAnlegen = profil.rollen.indexOf("PROJEKTLEITER") >= 0
            || profil.rollen.indexOf("ADMIN") >= 0;

        const alle = projekte.daten || [];
        const gefiltert = filter === "ALLE"
            ? alle
            : alle.filter(projekt => projekt.status === filter);

        return h(MUI.Box, null,

            h(MUI.Box, {sx: {display: "flex", flexWrap: "wrap", gap: 2,
                justifyContent: "space-between", alignItems: "center", mb: 1}},
                h(MUI.Box, null,
                    h(MUI.Typography, {variant: "h5", component: "h1"}, "Projekte"),
                    h(MUI.Typography, {variant: "body2", color: "text.secondary"},
                        "Projekte des Mandanten " + profil.mandantName
                        + ", für die Sie berechtigt sind.")),
                darfAnlegen
                    ? h(MUI.Button, {variant: "contained", onClick: () => setDialogOffen(true)},
                        "Neues Projekt")
                    : null),

            h(MUI.Box, {sx: {mb: 3}},
                h(MUI.ToggleButtonGroup, {
                    size: "small", exclusive: true, value: filter,
                    "aria-label": "Projekte filtern",
                    onChange: (ereignis, wert) => {
                        if (wert) {
                            setFilter(wert);
                        }
                    }
                },
                    h(MUI.ToggleButton, {value: "AKTIV"},
                        "Aktiv (" + alle.filter(p => p.status === "AKTIV").length + ")"),
                    h(MUI.ToggleButton, {value: "ARCHIVIERT"},
                        "Archiviert (" + alle.filter(p => p.status === "ARCHIVIERT").length + ")"),
                    h(MUI.ToggleButton, {value: "ALLE"}, "Alle (" + alle.length + ")"))),

            projekte.status === "laedt" && !projekte.daten
                ? h(B.Ladeanzeige, {text: "Projekte werden geladen …"})
                : null,

            projekte.status === "fehler"
                ? h(B.Fehlerhinweis, {
                    fehler: projekte.fehler,
                    titel: "Projekte konnten nicht geladen werden",
                    beiWiederholung: projekte.neuLaden
                })
                : null,

            projekte.status === "erfolg" && gefiltert.length === 0
                ? h(MUI.Paper, {variant: "outlined"},
                    h(B.Leerhinweis, {
                        text: alle.length === 0
                            ? "Ihnen ist derzeit kein Projekt zugeordnet."
                            : "In dieser Auswahl gibt es kein Projekt."
                    }))
                : null,

            gefiltert.length > 0
                ? h(MUI.Grid, {container: true, spacing: 2},
                    gefiltert.map(projekt =>
                        h(MUI.Grid, {item: true, xs: 12, md: 6, lg: 4, key: projekt.id},
                            h(Projektkachel, {projekt: projekt}))))
                : null,

            dialogOffen
                ? h(ProjektDialog, {
                    projekt: null,
                    beiSchliessen: () => setDialogOffen(false),
                    beiGespeichert: gespeichert => {
                        setDialogOffen(false);
                        window.location.hash = "#/projekte/" + gespeichert.id;
                    }
                })
                : null);
    }

    PP.Projektuebersicht = Projektuebersicht;
    PP.ProjektDialog = ProjektDialog;
})();
