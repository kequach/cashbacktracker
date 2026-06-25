# Cashback Tracker

Cashback Tracker ist eine Android-App zum lokalen Verwalten von Cashback- und
Gratis-testen-Aktionen.

Die App hilft dir dabei, den Ueberblick zu behalten: Welche Aktion hast du
geplant, welche wurde bereits eingereicht, und welches Cashback wurde schon
ueberwiesen?

## Funktionen

- Cashback-Aktionen mit Link, Produktname, Einloesezeitraum, Kaufpreis,
  Kaufkonto, Auszahlungskonto, Geraet und Notizen speichern.
- Aktionen als `Geplant`, `Eingereicht` oder `Ueberwiesen` markieren.
- Status direkt in der Datenliste durch Antippen der Karte wechseln.
- Cashback-Links auf Wunsch analysieren und Produktname sowie Zeitraum
  automatisch vorausfuellen lassen, wenn die Seite genug Informationen liefert.
- Bereits verwendete Konten und Geraete fuer dieselbe Aktion farblich markieren.
- Wiederholte Eingaben schneller ausfuellen durch Vorschlaege aus bisherigen
  Cashback-Aktionen.
- Bankkonten und Geraete lokal als Stammdaten verwalten.
- Cashback-Daten als CSV exportieren.
- Optionale Jubelanimationen fuer neue Eintraege, ueberwiesene Cashbacks und
  Erstattungsmeilensteine.

## Installation

Lade die APK aus dem neuesten GitHub Release herunter:

[GitHub Releases](https://github.com/kequach/cashbacktracker/releases)

Oeffne die APK auf deinem Android-Geraet und bestaetige die Installation. Je
nach Android-Version musst du vorher erlauben, Apps aus dieser Quelle zu
installieren.

Bei Updates installierst du einfach eine neuere APK ueber die vorhandene App.
Deinstalliere die App nicht, wenn du deine lokalen Daten behalten moechtest.

## Erste Schritte

1. Oeffne den Tab `Stammdaten`.
2. Lege deine Bankkonten an. Du kannst Spitznamen vergeben, damit du sie spaeter
   schneller erkennst.
3. Lege die Geraete an, mit denen du Cashback-Aktionen einloest.
4. Oeffne den Tab `Eingabe`.
5. Fuege den Cashback-Link ein und nutze optional `URL analysieren`.
6. Ergaenze Produktname, Zeitraum, Kaufpreis, Kaufkonto, Auszahlungskonto,
   Geraet und Notizen.
7. Speichere die Aktion als `Geplant` oder `Eingereicht`.
8. Im Tab `Daten` findest du alle Aktionen und kannst den Status per Antippen
   weiterstellen.

## Status

- `Geplant`: Du moechtest die Aktion noch kaufen oder einreichen.
- `Eingereicht`: Du hast die Aktion eingereicht und wartest auf die Auszahlung.
- `Ueberwiesen`: Das Cashback wurde ausgezahlt.

Wenn du dich vertippst, tippe die Aktion einfach weiter an. Der Status wechselt
der Reihe nach durch `Geplant`, `Eingereicht` und `Ueberwiesen`.

## Daten und Datenschutz

- Alle App-Daten bleiben lokal auf deinem Geraet.
- Es gibt keine Cloud-Synchronisierung, keine Werbung und keine Analytics.
- IBANs, Kontoinhaber, Geraetenotizen und Cashback-Notizen werden verschluesselt
  gespeichert.
- Die URL-Analyse greift nur dann auf eine Webseite zu, wenn du aktiv
  `URL analysieren` drueckst.
- Der CSV-Export ist absichtlich unverschluesselt. Die exportierte Datei enthaelt
  lesbare Cashback-Daten, IBANs und Notizen.

## CSV-Export

Im Tab `Daten` kannst du deine Eintraege als CSV exportieren. Nutze den Export,
wenn du deine Daten in einer Tabellenkalkulation ansehen oder manuell sichern
moechtest.

Behandle die CSV-Datei wie ein sensibles Dokument, weil sie lesbare Bankdaten und
Notizen enthalten kann.

## Aktuelle Grenzen

- Die App ist auf 100 Prozent Cashback ausgelegt. Der Kaufpreis ist deshalb auch
  der erwartete Erstattungsbetrag.
- Die URL-Analyse ist ein Best-effort-Helfer. Manche Webseiten liefern keine gut
  auslesbaren Daten.
- Es gibt aktuell keinen Import und keine Cloud-Synchronisierung.
- Es gibt keine Passwortverwaltung, kein Autofill und keine Browser-Integration.

## Weitere Informationen

- Aenderungen pro Version: [CHANGELOG.md](CHANGELOG.md)
- Technische Details fuer Entwicklung, Builds, Releases und CI:
  [DEVELOPMENT.md](DEVELOPMENT.md)
