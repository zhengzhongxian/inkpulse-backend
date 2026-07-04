package com.inkpulse.config.routing;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        // K8s postgres-read-service handles load balancing between replica pods
        return isReadOnly ? DataSourceType.REPLICA : DataSourceType.PRIMARY;
    }
}
