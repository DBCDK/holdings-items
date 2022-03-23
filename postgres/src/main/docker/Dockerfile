FROM docker-dbc.artifacts.dbccloud.dk/dbc-postgres-13:latest
MAINTAINER de-team

ENV POSTGRES_DB=holdingsitems
ENV POSTGRES_USER=holdingsitems
ENV POSTGRES_PASSWORD=holdingsitems

USER root
RUN chown -R $POSTGRES_LINUX_USER:$POSTGRES_LINUX_USER /docker-entrypoint.d/
USER $POSTGRES_LINUX_USER

LABEL POSTGRES_DB="Name of db (optional). Default is 'holdingsitems'" \
      POSTGRES_USER="(optional). Default is 'holdingsitems'" \
      POSTGRES_PASSWORD="(optional). Default is 'holdingsitems'"
