FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:latest
MAINTAINER os-team

COPY target/docker/*.jar /opt/payara6/jars/

ENV JAVA_MAX_HEAP_SIZE=1G \
    MAX_POOL_SIZE=8 \
    CONTEXT_ROOT=/ \
    HAZELCAST_CLUSTER=

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Url pointing at holdings items postgres (required). Ex.: 'user:pass@host:1234/dbname'" \
      COREPO_SOLR_URL="zk:// type url of the solr containing the holdings (required)" \
      CONTEXT_ROOT="Path of service" \
      UPDATE_SUPPLIER="Supplier (queue_rules table) to use for updates (default: UPDATE)" \
      UPDATE_ORIGINAL_SUPPLIER="Supplier (queue_rules table) to use for updates ignoring supersedes (default: UPDATE_ORIGINAL)" \
      ONLINE_SUPPLIER="Supplier (queue_rules table) to use for online updates (default: ONLINE)" \
      ONLINE_ORIGINAL_SUPPLIER="Supplier (queue_rules table) to use for online updates ignoring supersedes (default: ONLINE_ORIGINAL)" \
      COMPLETE_SUPPLIER="Supplier (queue_rules table) to use for complete updates (default: COMPLETE)" \
      COMPLETE_ORIGINAL_SUPPLIER="Supplier (queue_rules table) to use for complete updates ignoring supersedes (default: COMPLETE_ORIGINAL)" \
      SUPERSEDES_SUPPLIER="supplier name when enqueuing (default: SUPERSEDES)" \
      DISABLE_AUTHENTICATION="(optional, default is false)" \
      IDP_URL="Url of forsrights service (remember trailing slash) (required, ignored if authentication is enabled)" \
      IDP_RIGHTS="productname,name right enabling access to update (required, ignored if authentication is enabled)" \
      HAZELCAST_CLUSTER="DNS for headless service that comprises this hazelcast cluster (required in prod)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
