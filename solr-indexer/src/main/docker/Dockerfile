FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:latest

COPY target/docker/*.jar /opt/payara6/jars/

ENV THREADS=4 \
    RETRIES=5 \
    APPLICATION_NAME=HoldingsItemsSolrDocStoreIndexer \
    USER_AGENT=${project.build.finalName}/${project.version} \
    MAX_POOL_SIZE=2

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Holdingsitems database url (required: user:pass@host:port/database)" \
      QUEUES="Comma separated list og queue names in order to drain (required)" \
      THREADS="Number of threads (default 4)" \
      RETRIES="Now many times to try to process a queue entry (default: 5)" \
      HOLDINGS_ITEMS_CONTENT_SERVICE_URL="holdings-items-content-service base url (required (ex. http://hics.example.com:8080/))" \
      SOLR_DOC_STORE_URL="solr-doc-store base url (required (ex. http://sds.example.com:8080/))" \
      DATABASE_THROTTLE="Database connect error throttle (default: 1/5s,3/m,5/10m)" \
      FAILURE_THROTTLE="Processing failure throttle (default: 2/100ms,3/s,5/m)" \
      EMPTY_QUEUE_SLEEP="How long to sleep when queue is empty (default: 10s)" \
      QUEUE_WINDOW="How long to look back in time when pulling new messages (default: 500ms))" \
      MAX_QUERY_TIME="When query takes longer, recalc queryplan (default: 250ms)" \
      IDLE_RESCAN_EVERY="How often to rediscover start time when idling (default: 5)" \
      RESCAN_EVERY="How often to rediscover start time when running (default: 500)" \
      MAX_TRIES="Now many times to try to process a queue entry (default: 3)" \
      MAX_POOL_SIZE="Number of database connections to allocate (default: 2, atleast threads + 1)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
