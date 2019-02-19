FROM docker.dbc.dk/payara5-full:latest
MAINTAINER os-team

# Not needed now?
ENV KAFKA_LOG_APPNAME holdings-items-update-webservice

ENV APPLICATION_NAME holdings-items-update-webservice
ENV CONTEXT_ROOT /

ENV MAX_AGE_MINUTES 480

ENV MAX_POOL_SIZE 8
ENV POOL_RESIZE_QUANTITY 2
ENV STEADY_POOL_SIZE 2
ENV LOG__dk_dbc INFO

COPY holdings-items-update-webservice-1.1.3-SNAPSHOT.war holdings-items-update.json deployments/

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Url pointing at holdings items postgres (required). Ex.: 'user:pass@host:1234/dbname'" \
      SERVER_NAME="Name of server (hostname) (for wsdl)" \
      CONTEXT_ROOT="Path of service (for wsdl)" \
      UPDATE_QUEUE_LIST="Comma separated list of target queue names for update call (suffixed by ':old' queue on q table) (required)" \
      ONLINE_QUEUE_LIST="Comma separated list of target queue names for online call (suffixed by ':old' queue on q table) (required)" \
      COMPLETE_QUEUE_LIST="Comma separated list of target queue names for complete call (suffixed by ':old' queue on q table) (required)" \
      WARNING_IS_FATAL="(optional, default is false)" \
      DISABLE_AUTHENTICATION="(optional, default is false)" \
      FORS_RIGHTS_URL="Url of forsrights service (remember trailing slash) (required if authentication is enabled)" \
      RIGHTS_GROUP="Authentication group (required if authentication is enabled)" \
      RIGHTS_NAME="Authentication Name (required if authentication is enabled)" \
      MAX_AGE_MINUTES="Forsrights cache duration (optional, default is 480 minutes / 8 hours)" \
      MAX_POOL_SIZE="JDBC connection pool parameter (optional, default is 8)" \
      POOL_RESIZE_QUANTITY="JDBC connection pool parameter (optional, default is 2)" \
      STEADY_POOL_SIZE="JDBC connection pool parameter (optional, default is 2)" \
      DEBUG_XML_AGENCIES="List of agencies that should have logged requests"
