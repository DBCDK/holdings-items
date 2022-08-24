#!/bin/bash -x

(
    echo "\set ON_ERROR_STOP"
    echo "DELETE FROM queue_rules;"

    for queue in ${QUEUE_RULES}; do
        IFS=, eval 'parts=( $queue )'
        suppliers="${parts[0]}"
        consumers="${parts[1]}"
        postpone=$(( ${parts[2]} + 0 ))
        for supplier in ${suppliers//|/ }; do
            for consumer in ${consumers//|/ }; do
                echo "INSERT INTO queue_rules (supplier, consumer, postpone) VALUES( '${supplier//\'/\'\'}', '${consumer//\'/\'\'}', $postpone );"
            done
        done
    done
) | PGPASSWORD=$POSTGRES_PASSWORD psql -U $POSTGRES_USER -d $POSTGRES_DB
