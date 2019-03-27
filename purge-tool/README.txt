
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
missing mandatory options: agencyid, worker, openagencyurl, db
Usage: prog [ options ]

--agencyid=argument      [1:1] Agency ID to purge for
--commit(=argument)      [0:1] commit every n times. Default is 0 meaning commit only at end
--db=argument            [1:1] connectstring for database. E.g jdbc:postgresql://user:password@host:port/database
--debug                  [0:1] turn on debug logging
--dry-run                [0:1] Do not commit anything
--openagencyurl=argument [1:1] OpenAgency URL to connect to. E.g. http://openagency.addi.dk/<version>/
--worker=argument        [1:1] Worker to be enqueued to


java -jar target/holdings-items-purge-tool.jar --agencyid=133300 --openagencyurl=http://openagency.addi.dk/2.34/ --db=jdbc:postgresql://holdings:holdings@192.168.56.2:5432/holdings --worker=solr


Udestående:
==========
  Skal poster, som allerede er 'Decommisioned', lægges på kø til indeksering?

  Skal HoldingsItemsDAO.enqueue(..., long milliSeconds) felt bruges til kø ?

  Skal værktøj tilbyde at slette records hele fra database, hvis alle records allerede er 'Decommisioned'. ( evt med check mod SOLR)

