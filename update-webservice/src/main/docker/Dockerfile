FROM docker.dbc.dk/dbc-payara
MAINTAINER os-team

ENV KAFKA_LOG_APPNAME holdings-items-update-webservice

ADD pre/* $PAYARA_CFG/pre/
ADD post/* $PAYARA_CFG/post/

LABEL HOLDINGS_ITEMS_POSTGRES_URL="Url pointing at holdings items postgres (required). Ex.: 'user:pass@host:1234/dbname'" \
      SERVER_NAME="Name of server (hostname) (for wsdl)" \
      CONTEXT_ROOT="Path of service (for wsdl)" \
      UPDATE_QUEUE_LIST="Comma separated list of target queue names for update call (suffixed by ':old' queue on q table) (required)" \
      ONLINE_QUEUE_LIST="Comma separated list of target queue names for online call (suffixed by ':old' queue on q table) (required)" \
      COMPLETE_QUEUE_LIST="Comma separated list of target queue names for complete call (suffixed by ':old' queue on q table) (required)" \
      WARNING_IS_FATAL="(optional, default is false)" \
      DISABLE_AUTHENTICATION="(optional, default is true)" \
      X_FORWARDED_FOR="(optional)" \
      FORSRIGHTS_URL="Url of forsrights service (remember trailing slash) (required if authentication is enabled)" \
      RIGHTS_GROUP="Authentication group (required if authentication is enabled)" \
      RIGHTS_NAME="Authentication Name (required if authentication is enabled)" \
      MAX_POOL_SIZE="JDBC connection pool parameter (optional, default is 8)" \
      POOL_RESIZE_QUANTITY="JDBC connection pool parameter (optional, default is 2)" \
      STEADY_POOL_SIZE="JDBC connection pool parameter (optional, default is 2)"

ADD holdings-items-update-webservice-*-SNAPSHOT.war $PAYARA_CFG/pre/holdings-items-update-webservice.war
RUN payara-setup $PAYARA_CFG/pre/
