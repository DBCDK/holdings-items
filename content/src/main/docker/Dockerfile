FROM docker.dbc.dk/payara5-micro:latest
MAINTAINER os-team

ENV MAX_POOL_SIZE=8

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Url pointing at holdings items postgres (required). Ex.: 'user:pass@host:1234/dbname'" \
      MAX_POOL_SIZE="JDBC connection pool parameter (optional, default is 8)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
