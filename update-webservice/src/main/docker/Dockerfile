FROM docker.dbc.dk/payara5-full:latest
MAINTAINER os-team

ENV MAX_POOL_SIZE=32

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Url pointing at holdings items postgres (required). Ex.: 'user:pass@host:1234/dbname'" \
      SERVER_NAME="Name of server (hostname) (for wsdl)" \
      CONTEXT_ROOT="Path of service (for wsdl)" \
      UPDATE_QUEUE_LIST="Comma separated list of target queue names for update call (suffixed by ':old' queue on q table) (required)" \
      ONLINE_QUEUE_LIST="Comma separated list of target queue names for online call (suffixed by ':old' queue on q table) (required)" \
      COMPLETE_QUEUE_LIST="Comma separated list of target queue names for complete call (suffixed by ':old' queue on q table) (required)" \
      DISABLE_AUTHENTICATION="(optional, default is false)" \
      FORS_RIGHTS_URL="Url of forsrights service (remember trailing slash) (required if authentication is enabled)" \
      FORS_RIGHT_CACHE_AGE="how long to cache forsright responses ie. 3s, 1h, or 3d" \
      RIGHTS_GROUP="Authentication group (required if authentication is enabled)" \
      RIGHTS_NAME="Authentication Name (required if authentication is enabled)" \
      MAX_POOL_SIZE="JDBC connection pool parameter (optional, default is 32)" \
      DEBUG_XML_AGENCIES="List of agencies that should have logged requests"

COPY target/${project.build.finalName}.war target/docker/app.json deployments/
