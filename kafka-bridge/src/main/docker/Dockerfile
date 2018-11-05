FROM docker.dbc.dk/payara5-micro:os

ENV THREADS=1 \
    RETRIES=5 \
    APPLICATION_NAME=HoldingsItemsKafkaBridge

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Holdingsitems database url (required: user:pass@host:port/database)" \
      THREADS="Number of threads (default 1)" \
      KAFKA_SERVER="Broker list for kafka (required)" \
      KAFKA_TOPIC="Where in kafka to put the messages (required)" \
      QUEUES="Comma seperated list of queues to drain in order (required)" \
      DATABASE_THROTTLE="Database connect error throttle (default: 1/5s,3/m,5/10m)" \
      FAILURE_THROTTLE="Processing failure throttle (default: 2/100ms,3/s,5/m)" \
      EMPTY_QUEUE_SLEEP="How long to sleep when queue is empty (default: 10s)" \
      QUEUE_WINDOW="How long to look back in time when pulling new messages (default: 500ms))" \
      MAX_QUERY_TIME="When query takes longer, recalc queryplan (default: 250ms)" \
      IDLE_RESCAN_EVERY="How often to rediscover start time when idling (default: 5)" \
      RESCAN_EVERY="How often to rediscover start time when running (default: 500)" \
      MAX_TRIES="Now many times to try to process a queue entry (default: 3)"

ADD config.d/* config.d
ADD *.war wars/
