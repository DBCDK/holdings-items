FROM docker.dbc.dk/dbc-postgres:10
MAINTAINER os-team

ENV POSTGRES_DB=holdingsitems
ENV POSTGRES_USER=holdingsitems
ENV POSTGRES_PASSWORD=holdingsitems

USER root
RUN chown -R $POSTGRES_LINUX_USER:$POSTGRES_LINUX_USER /docker-entrypoint.d/
USER $POSTGRES_LINUX_USER

LABEL POSTGRES_DB="Name of db (optional). Default is 'holdingsitems'" \
      POSTGRES_USER="(optional). Default is 'holdingsitems'" \
      POSTGRES_PASSWORD="(optional). Default is 'holdingsitems'"
