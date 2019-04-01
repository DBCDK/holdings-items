
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


java -jar target/holdings-items-purge-tool.jar --agency-id=133300 --open-agency-url=http://openagency.addi.dk/2.34/ --database=jdbc:postgresql://holdings:holdings@192.168.56.2:5432/holdings --queue=solr


Udestående:
==========
  Skal poster, som allerede er 'Decommisioned', lægges på kø til indeksering?

  Skal HoldingsItemsDAO.enqueue(..., long milliSeconds) felt bruges til kø ?

  Skal værktøj tilbyde at slette records hele fra database, hvis alle records allerede er 'Decommisioned'. ( evt med check mod SOLR)

