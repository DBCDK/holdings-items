FROM docker.dbc.dk/payara5-micro:latest

ENV APPLICATION_NAME=HoldingsItemsMonitor \
    MAX_POOL_SIZE=2

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Holdingsitems database url (required: user:pass@host:port/database)" \
      MAX_POOL_SIZE="Number of database connections to allocate (default: 2)"

ADD *.json *.war deployments/
