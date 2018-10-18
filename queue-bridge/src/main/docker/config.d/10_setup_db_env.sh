add_jdbc_resource_from_url "jdbc/holdings-items" ${HOLDINGS_ITEMS_POSTGRES_URL} steady-pool-size=0 max-pool-size=$((1 + ${THREADS}))
