FROM docker-dbc.artifacts.dbccloud.dk/dbc-postgres-13
MAINTAINER os-team

ENV POSTGRES_DB=holdingsitems
ENV POSTGRES_USER=holdingsitems
ENV POSTGRES_PASSWORD=holdingsitems

LABEL POSTGRES_DB="Name of db (optional). Default is 'holdingsitems'" \
      POSTGRES_USER="(optional). Default is 'holdingsitems'" \
      POSTGRES_PASSWORD="(optional). Default is 'holdingsitems'" \
      QUEUE_RULES="Whitespace seperated list of 'supplier,consumer[,postponed]' where supplier,consumer can contain '|' to expand multiple values ex 'UPDATE|ONLINE|COMPLATE,horse|cow'

COPY --chown=postgres:postgres src/main/docker/docker-entrypoint.d/* target/holdingsitems.sql /docker-entrypoint.d/
