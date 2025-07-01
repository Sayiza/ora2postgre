# Experiments 4 comigration

## Work in progress:

Objecttype spec is parsed and converted to java and postgre, not fully functional 
immediate work is marked with TODO
overall work to be done is to 
code all ast visitors, 
impement to_java and to_postgre for all
create maps for dependencies and adjusted names and inject them in the export step?
do not inject, just pass with trough (adviced by ai)
for tables and view tranformators need to be put into the sql folder
write conditional function/procedure
finish fixin grammar for all packages
example work flow of a db call in a for loop
example work flow for possible htp.p call
docker rm -f pgtest; docker run --name pgtest -e POSTGRES_PASSWORD=secret -p 5432:5432 -d postgres; rm -r ~/dev/co-mig-target4/postgre/
curl -X POST http://localhost:8080/migration/full
...

## Kontext 

Aufteilung auf mehrere verknuepfte Unterprojekte
1 Plsql wird zu pgplsql transpiliert, so weit dass eine 1-1 Weiterverwendung von VIEWS moeglich ist
2 Plsql wird vollstaendig zu Java transpiliert, relevanter Code wird als Restclient exponiert
3 AI basierte semi-automatische transformation von CoFramework zu Angular
4 AI basierte semi-automatische Ueberarbeitung des Backends
5 Die eingentliche Tabellen und Datenübermittlung erfolgt mit externer Software wie AWS-Schemamigrator

1: Ziel:
Views bleiben verwendbar; 
Komplexe APIs (Anmeldecheck, SPO Funktionen, Antrittszaehlung) können (vorerst) bleiben.
Notwendige Schritte: 
Rohdaten werden aus Oracle ausgelesen
Mit Antlr wird der Code geparset und ein AST 
Aus dem AST wird ein Semantischer Baum erstellt, 
Packet-Typen, Objekt-Typen, Funktionen, Packet als Prefix, etc. 
Synonyme benutzen fuer eine Schema-Map fuer alle Objekte, Tabellen, Views, Funktionen, etc
Funktionen als Stubs anlegen, evtl. Abhaengigkeitsbaum, Funktionen mit Code transpilieren, 
Evtl. nicht automatisch transformierbare Views mit AI semi-automatisch ersetzen und hinterlegen
2: Ziel:
Das bestehende CO-Framework funktioniert weiterhin, aber in Java;
Applikationen können dann nach und nach migriert werden.
Notwendige Schritte:
Packet wird Objekt, nicht statische Klasse; 
Objektypen und Pakete werden Objekte; 
Namesmapping mit kompletten extrahierten Daten, unter Berücksichtigung der Synonyme; 
Neu erstellert Code wird automatisch transformiert beim Solven, 
Wo fehlerhaft mit AI ersetzen und hinterlegen, wie etwa connect by prior Probleme und 
Besonderheiten wie Sessionmanagement und Batchjobs behandeln
3,4: Experimente wie weit man mit direktem Input von Code kommt: maeßig
AI EMPFIEHLT CODE ALS AST zur AI gestuetzen Konvertierung zuzufuehren!!!
Noch nicht begonnen: Selbsthosting vs Cloud, Pipleline aufbauen, 
Trainingsbeispiele mit Cookbook aufbereiten,
Eindruck: Ist eher als Unterstützung bei manueller Migration sinnvoll!
5: Paralleles Projekt

Resultat 1 und 2: 
Ist vielverspechend, aber mit Risiko verbunden:
Erst ca. 1-5% fertig. 
1 Shot-Transformation hat noch niemand geschafft!
Erst klar ob es performant funktioniert wenn es 100% fertig ist.
Entscheidung notwendig ob man nur 1 anstrebt, um bestehende Views weiter verwenden zu koennen
Oder ob man 2 anstebt, d.h. eine (voruebergehende) Verwendung eines "Java-Co2-Frameworks"

Alternativer Ansatz für Folgeprojekt:
Mit Schritt 5 fortfahren: Datenbank mit Tabellen, und INHALTEN portieren
Bestehende CONX Container generisch umschaltbar auf Postgres mache
"PINKLINE" paralell zu RED/BLUE, etc. wo CONX auf Postgre laeuft
Bestehende "Schulden" bereinigen und bestehenden CONX Code lauffähig machen 
Bzw. zumindest die bestehenden Schulden zu erfassen und zu delegieren
Evaluierung ob mit Code-Generierung (aufbauend auf diesem Projekt) Hilfe moeglich ist:
z.B. Entity-Repo Layer oder interne Services AUTOMATISCH zu erstellen.
z.B. CO-PV-INT, CO-SLC-XM-INT, CO-STUD-INT enthalten interne Services,
z.B. CO-XM-CORE nur fuer Public APIS (?schreibende zugriffe die komplexe Regeln haben?)