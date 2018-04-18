FROM docker.dbc.dk/payara-micro:LogAsJson

ENV APPLICATION_NAME=HoldingsItemsMonitor \
    ADD_PAYARA_ARGS=--nocluster

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Holdingsitems database url (required: user:pass@host:port/database)"

ADD config.d/* config.d
ADD *.war wars/
