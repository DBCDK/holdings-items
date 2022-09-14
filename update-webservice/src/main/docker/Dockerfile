FROM docker-dbc.artifacts.dbccloud.dk/payara5-full:latest
MAINTAINER os-team

COPY target/docker/*.jar /opt/payara5/jars/

ENV MAX_POOL_SIZE=32

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Url pointing at holdings items postgres (required). Ex.: 'user:pass@host:1234/dbname'" \
      SERVER_NAME="Name of server (hostname) (for wsdl)" \
      CONTEXT_ROOT="Path of service (for wsdl)" \
      UPDATE_SUPPLIER="Supplier (queue_rules table) to use for updates (default: UPDATE)" \
      UPDATE_ORIGINAL_SUPPLIER="Supplier (queue_rules table) to use for updates ignoring supersedes (default: UPDATE_ORIGINAL)" \
      ONLINE_SUPPLIER="Supplier (queue_rules table) to use for online updates (default: ONLINE)" \
      ONLINE_ORIGINAL_SUPPLIER="Supplier (queue_rules table) to use for online updates ignoring supersedes (default: ONLINE_ORIGINAL)" \
      COMPLETE_SUPPLIER="Supplier (queue_rules table) to use for complete updates (default: COMPLETE)" \
      COMPLETE_ORIGINAL_SUPPLIER="Supplier (queue_rules table) to use for complete updates ignoring supersedes (default: COMPLETE_ORIGINAL)" \
      DISABLE_AUTHENTICATION="(optional, default is false)" \
      IDP_URL="Url of forsrights service (remember trailing slash) (required if authentication is enabled)" \
      IDP_RIGHTS="productname,name right enabling access to update (required if authentication is enabled)" \
      MAX_POOL_SIZE="JDBC connection pool parameter (optional, default is 32)" \
      DEBUG_XML_AGENCIES="List of agencies that should have logged requests, -1 is all (optional)"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
