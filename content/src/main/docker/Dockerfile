FROM docker-dbc.artifacts.dbccloud.dk/payara5-micro:latest
MAINTAINER os-team

COPY target/docker/*.jar /opt/payara5/jars/

ENV JAVA_MAX_HEAP_SIZE=1G
ENV MAX_POOL_SIZE=8
ENV CONTEXT_ROOT=/

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Url pointing at holdings items postgres (required). Ex.: 'user:pass@host:1234/dbname'" \
      COREPO_SOLR_URL="zk:// type url of the solr containing the holdings (required)" \
      CONTEXT_ROOT="Path of service" \
      SUPERSEDES_SUPPLIER="supplier name when enqueuing (default: SUPERSEDES)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
