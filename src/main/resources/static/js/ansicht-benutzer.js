/*
 * Ansicht "Benutzerverwaltung" (#/benutzer).
 *
 * Nur fuer Administratoren erreichbar. Die Ansicht ist in der Navigation
 * ausgeblendet, wenn die Rolle fehlt; verbindlich schuetzt jedoch der Server,
 * der alle Aufrufe unter /api/benutzer auf die Rolle ADMIN beschraenkt.
 */
(function () {
    "use strict";

    const PP = window.PP;
    const B = PP.bausteine;
    const h = React.createElement;
    const MUI = window.MaterialUI;

    const ALLE_ROLLEN = ["ADMIN", "PROJEKTLEITER", "MITARBEITER"];

    // ------------------------------------------------------------------
    // Dialog: Konto anlegen / bearbeiten
    // ------------------------------------------------------------------

    function BenutzerDialog(eigenschaften) {
        const bearbeitet = Boolean(eigenschaften.benutzer);
        const benutzer = eigenschaften.benutzer || {};

        const [formular, setFormular] = React.useState({
            email: benutzer.email || "",
            vorname: benutzer.vorname || "",
            nachname: benutzer.nachname || "",
            passwort: "",
            rollen: benutzer.rollen ? benutzer.rollen.slice() : ["MITARBEITER"],
            aktiv: bearbeitet ? benutzer.aktiv : true
        });
        const [fehler, setFehler] = React.useState(null);
        const [speichert, setSpeichert] = React.useState(false);

        function aendere(feld, wert) {
            setFormular(alt => Object.assign({}, alt, {[feld]: wert}));
        }

        function rolleUmschalten(rolle) {
            setFormular(alt => {
                const enthalten = alt.rollen.indexOf(rolle) >= 0;
                return Object.assign({}, alt, {
                    rollen: enthalten
                        ? alt.rollen.filter(eintrag => eintrag !== rolle)
                        : alt.rollen.concat(rolle)
                });
            });
        }

        async function speichern(ereignis) {
            ereignis.preventDefault();
            setSpeichert(true);
            setFehler(null);
            try {
                if (bearbeitet) {
                    await PP.api.benutzerAendern(benutzer.id, {
                        vorname: formular.vorname,
                        nachname: formular.nachname,
                        rollen: formular.rollen,
                        aktiv: formular.aktiv,
                        // Leeres Feld bedeutet "Kennwort unverändert lassen"; es
                        // wird daher gar nicht uebertragen.
                        neuesPasswort: formular.passwort === "" ? null : formular.passwort
                    });
                } else {
                    await PP.api.benutzerAnlegen({
                        email: formular.email,
                        vorname: formular.vorname,
                        nachname: formular.nachname,
                        passwort: formular.passwort,
                        rollen: formular.rollen
                    });
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
                    bearbeitet ? "Benutzerkonto bearbeiten" : "Benutzerkonto anlegen"),
                h(MUI.DialogContent, {dividers: true},

                    fehler && Object.keys(feldfehler).length === 0
                        ? h(MUI.Alert, {severity: "error", sx: {mb: 2}}, fehler.message)
                        : null,

                    h(MUI.TextField, {
                        label: "E-Mail-Adresse (Anmeldename)",
                        type: "email", value: formular.email,
                        onChange: e => aendere("email", e.target.value),
                        fullWidth: true, required: !bearbeitet, margin: "normal",
                        disabled: bearbeitet,
                        autoFocus: !bearbeitet,
                        error: Boolean(feldfehler.email),
                        helperText: feldfehler.email
                            || (bearbeitet ? "Der Anmeldename ist nicht änderbar." : " ")
                    }),
                    h(MUI.Box, {sx: {display: "flex", gap: 2, flexWrap: "wrap"}},
                        h(MUI.TextField, {
                            label: "Vorname", value: formular.vorname,
                            onChange: e => aendere("vorname", e.target.value),
                            required: true, margin: "normal", sx: {flex: 1, minWidth: 180},
                            error: Boolean(feldfehler.vorname),
                            helperText: feldfehler.vorname || " "
                        }),
                        h(MUI.TextField, {
                            label: "Nachname", value: formular.nachname,
                            onChange: e => aendere("nachname", e.target.value),
                            required: true, margin: "normal", sx: {flex: 1, minWidth: 180},
                            error: Boolean(feldfehler.nachname),
                            helperText: feldfehler.nachname || " "
                        })),
                    h(MUI.TextField, {
                        label: bearbeitet ? "Neues Kennwort (optional)" : "Kennwort",
                        type: "password", value: formular.passwort,
                        onChange: e => aendere("passwort", e.target.value),
                        fullWidth: true, required: !bearbeitet, margin: "normal",
                        autoComplete: "new-password",
                        error: Boolean(feldfehler.passwort || feldfehler.neuesPasswort),
                        helperText: feldfehler.passwort || feldfehler.neuesPasswort
                            || "Mindestens 10 Zeichen."
                    }),

                    h(MUI.FormControl, {
                        component: "fieldset", margin: "normal",
                        error: Boolean(feldfehler.rollen)
                    },
                        h(MUI.FormLabel, {component: "legend"}, "Rollen"),
                        h(MUI.FormGroup, null,
                            ALLE_ROLLEN.map(rolle =>
                                h(MUI.FormControlLabel, {
                                    key: rolle,
                                    control: h(MUI.Checkbox, {
                                        checked: formular.rollen.indexOf(rolle) >= 0,
                                        onChange: () => rolleUmschalten(rolle)
                                    }),
                                    label: B.ROLLENNAMEN[rolle]
                                }))),
                        h(MUI.FormHelperText, null,
                            feldfehler.rollen || "Mehrfachauswahl ist möglich.")),

                    bearbeitet
                        ? h(MUI.FormControlLabel, {
                            sx: {display: "block", mt: 1},
                            control: h(MUI.Switch, {
                                checked: formular.aktiv,
                                onChange: e => aendere("aktiv", e.target.checked)
                            }),
                            label: formular.aktiv
                                ? "Konto ist aktiv" : "Konto ist deaktiviert"
                        })
                        : null),
                h(MUI.DialogActions, null,
                    h(MUI.Button, {onClick: eigenschaften.beiSchliessen, disabled: speichert},
                        "Abbrechen"),
                    h(MUI.Button, {
                        type: "submit", variant: "contained", disabled: speichert,
                        startIcon: speichert
                            ? h(MUI.CircularProgress, {size: 16, color: "inherit"}) : null
                    }, bearbeitet ? "Änderungen speichern" : "Konto anlegen"))));
    }

    // ------------------------------------------------------------------
    // Übersicht
    // ------------------------------------------------------------------

    function Benutzerverwaltung(eigenschaften) {
        const profil = eigenschaften.profil;
        const konten = B.verwendeAnfrage(() => PP.api.benutzer(), []);
        const [dialog, setDialog] = React.useState(null);

        const liste = konten.daten || [];

        return h(MUI.Box, null,

            h(MUI.Box, {sx: {display: "flex", flexWrap: "wrap", gap: 2,
                justifyContent: "space-between", alignItems: "center", mb: 3}},
                h(MUI.Box, null,
                    h(MUI.Typography, {variant: "h5", component: "h1"}, "Benutzerverwaltung"),
                    h(MUI.Typography, {variant: "body2", color: "text.secondary"},
                        "Konten und Rollen des Mandanten " + profil.mandantName + ".")),
                h(MUI.Button, {variant: "contained", onClick: () => setDialog({neu: true})},
                    "Konto anlegen")),

            konten.status === "laedt" && !konten.daten
                ? h(B.Ladeanzeige, {text: "Benutzerkonten werden geladen …"})
                : null,
            konten.status === "fehler"
                ? h(B.Fehlerhinweis, {
                    fehler: konten.fehler,
                    titel: "Benutzerkonten konnten nicht geladen werden",
                    beiWiederholung: konten.neuLaden
                })
                : null,

            liste.length > 0
                ? h(MUI.Paper, {variant: "outlined"},
                    h(MUI.TableContainer, null,
                        h(MUI.Table, null,
                            h(MUI.TableHead, null,
                                h(MUI.TableRow, null,
                                    h(MUI.TableCell, null, "Name"),
                                    h(MUI.TableCell, null, "Anmeldename"),
                                    h(MUI.TableCell, null, "Rollen"),
                                    h(MUI.TableCell, null, "Status"),
                                    h(MUI.TableCell, {align: "right"}, "Aktion"))),
                            h(MUI.TableBody, null,
                                liste.map(konto =>
                                    h(MUI.TableRow, {key: konto.id, hover: true},
                                        h(MUI.TableCell, null,
                                            konto.vollerName,
                                            konto.id === profil.benutzerId
                                                ? h(MUI.Typography, {
                                                    variant: "caption",
                                                    color: "text.secondary",
                                                    sx: {display: "block"}
                                                }, "eigenes Konto")
                                                : null),
                                        h(MUI.TableCell, {sx: {fontFamily: "monospace",
                                            fontSize: 13}}, konto.email),
                                        h(MUI.TableCell, null,
                                            h(MUI.Box, {sx: {display: "flex", gap: 0.5,
                                                flexWrap: "wrap"}},
                                                konto.rollen.map(rolle =>
                                                    h(MUI.Chip, {
                                                        key: rolle, size: "small",
                                                        label: B.ROLLENNAMEN[rolle],
                                                        color: rolle === "ADMIN"
                                                            ? "secondary" : "default",
                                                        variant: rolle === "ADMIN"
                                                            ? "filled" : "outlined"
                                                    })))),
                                        h(MUI.TableCell, null,
                                            h(MUI.Chip, {
                                                size: "small",
                                                label: konto.aktiv ? "Aktiv" : "Deaktiviert",
                                                color: konto.aktiv ? "success" : "default",
                                                variant: konto.aktiv ? "filled" : "outlined"
                                            })),
                                        h(MUI.TableCell, {align: "right"},
                                            h(MUI.Button, {
                                                size: "small",
                                                onClick: () => setDialog({benutzer: konto})
                                            }, "Bearbeiten"))))))))
                : null,

            dialog
                ? h(BenutzerDialog, {
                    benutzer: dialog.benutzer || null,
                    beiSchliessen: () => setDialog(null),
                    beiGespeichert: () => {
                        setDialog(null);
                        konten.neuLaden();
                    }
                })
                : null);
    }

    PP.Benutzerverwaltung = Benutzerverwaltung;
})();
