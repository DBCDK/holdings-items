FROM docker.dbc.dk/payara-micro:LogAsJson

ENV THREADS=4 \
    RETRIES=5 \
    APPLICATION_NAME=HoldingsItemsSolrDocStoreIndexer



LABEL HOLDINGS_ITEMS_POSTGRES_URL="Holdingsitems database url (required: user:pass@host:port/database)" \
       QUEUES="Comma separated list og queue names in order to drain (required)" \
       DATABASE_THROTTLE="Database connect error throttle (default: 1/5s,3/m,5/10m)" \
       THROTTLE="Processing failure throttle (default: 2/100ms,3/s,5/m)" \
       EMPTY_QUEUE_SLEEP="How long to sleep when queue is empty (default: 10000 (ms))" \
       MAX_QUERY_TIME="When query takes longer, recalc queryplan (default: 250 (ms))" \
       IDLE_RESCAN_EVERY="How often to rediscover start time when idling (default: 10)" \
       RESCAN_EVERY="How often to rediscover start time when running (default: 1000)" \
       THREADS="Number of threads (default 4)" \
       RETRIES="Now many times to try to process a queue entry (default: 5)" \
       SOLR_DOC_STORE_URL="solr-doc-store base url (required (ex. http://sds.example.com:8080/))"

ADD config.d/* config.d
ADD *.war wars/
