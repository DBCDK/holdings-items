#!/bin/bash

exec docker run --rm -ti --env-file <(
	echo COMPLETE_QUEUE_LIST=c,c:old
	echo CONTEXT_ROOT=/
	echo HOLDINGS_ITEMS_POSTGRES_URL=${PGUSER:-$USER}:${PGPASSWORD:-$USER}@`hostname -i`:${PGPORT:-5432}/${PGDATABASE:-$USER}
	echo LOG__dk_dbc=DEBUG
	echo ONLINE_QUEUE_LIST=o,o:old
	echo SERVER_NAME=`hostname -i`
	echo UPDATE_QUEUE_LIST=u,u:old
) -p 8800:8080 holdings-items-update-webservice-1.1.2-snapshot
