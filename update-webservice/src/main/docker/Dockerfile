FROM docker.dbc.dk/dbc-payara5-full
MAINTAINER os-team

ENV KAFKA_LOG_APPNAME holdings-items-update-webservice

USER root
RUN sed -i 's@^\(exec.*\)$@\1 $( [ -e $HOME/postbootcommandfile.txt ] \&\& echo --postbootcommandfile $HOME/postbootcommandfile.txt )@' /startup.sh
USER payara

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
      MAX_POOL_SIZE="JDBC connection pool parameter (optional, default is 8)" \
      POOL_RESIZE_QUANTITY="JDBC connection pool parameter (optional, default is 2)" \
      STEADY_POOL_SIZE="JDBC connection pool parameter (optional, default is 2)" \
      DEBUG_XML_AGENCIES="List of agencies that should have logged requests"

COPY --chown=payara:payara config/* $PAYARA_CFG_DIR/
COPY --chown=payara:payara *.war $PAYARA_CFG_DIR/app.war
