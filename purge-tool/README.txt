
Værktøj til at slette beholdninger for et agency

Sæt alle poster for agency til 'Decommissioned' og læg på kø til indeksering

Kan afprøves med parameter: --dry-run

Udfører:
=======
  Lav statusrapport for agency over eksisterende beholdning (antal i hver type, OnShelf, Decommissioned osv.)
  Vis agency navn.
  Bekræft sletning med agency navn
  Marker alle items som 'Decommissioned'. Læg på kø
  Commit
  Vent på at kø er behandlet
  Slet items og collections for agency fra holdings items database
  Commit
  Gentag statusrapport (Alle med 'Decommissioned')


Eksempel:
========
java -jar target/holdings-items-purge-tool.jar
Required options: a, d, o & q are missing

usage: java -jar holdings-items-purge-tool.jar -a <AGENCY> [-c <NUM>] -d
       <DB> [-h] [-n] -o <URL> -q <QUEUE> [-v]

    -a,--agency-id <AGENCY>       Agency ID to purge for
    -c,--commit-every <NUM>       How often to commit
    -d,--database <DB>            Connectstring for database. E.g
                                  jdbc:postgresql://user:password@host:port/
                                  database
    -h,--help                     This help
    -n,--dry-run                  Only simulate
    -o,--open-agency-url <URL>    OpenAgency URL to connect to. E.g.
                                  http://openagency.addi.dk/<version>/
    -q,--queue <QUEUE>            Name of queue that should process deletes
    -v,--verbose                  Enable debug log


java -jar target/holdings-items-purge-tool.jar --agency-id=133300 --open-agency-url=http://openagency.addi.dk/2.34/ --database=jdbc:postgresql://fbstest_holdings_items:<password>@db.holdingsitems.fbstest.prod.dbc.dk:5432/fbstest_holdings_items_db --queue=solr-doc-store-very-slow
evt: --dry-run
