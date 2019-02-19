FROM docker.dbc.dk/payara5-micro:latest

ENV THREADS=1 \
    RETRIES=5 \
    APPLICATION_NAME=HoldingsItemsQueueBridge \
    LOG__dk_dbc=INFO

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Holdingsitems database url (required: user:pass@host:port/database)" \
      RULES="worker pattern: Semicolon separated list of mqName=dbName (required)" \
      MAX_POOL_SIZE="Max number of connections to the database, should be THREADS+1 (default: 2)" \
      THREADS="Number of threads (default 1)" \
      QUEUE_SERVER="Address og message queue server (required (ex. sds.example.com:7676))" \
      DATABASE_THROTTLE="Database connect error throttle (default: 1/5s,3/m,5/10m)" \
      FAILURE_THROTTLE="Processing failure throttle (default: 2/100ms,3/s,5/m)" \
      EMPTY_QUEUE_SLEEP="How long to sleep when queue is empty (default: 10s)" \
      QUEUE_WINDOW="How long to look back in time when pulling new messages (default: 500ms))" \
      MAX_QUERY_TIME="When query takes longer, recalc queryplan (default: 250ms)" \
      IDLE_RESCAN_EVERY="How often to rediscover start time when idling (default: 5)" \
      RESCAN_EVERY="How often to rediscover start time when running (default: 500)" \
      MAX_TRIES="Now many times to try to process a queue entry (default: 3)"

COPY *.war *.json deployments/
