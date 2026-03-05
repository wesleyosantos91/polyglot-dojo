package io.github.wesleyosantos91.infrastructure.datastore.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(ReadWriteRoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        DataSourceType target = readOnly ? DataSourceType.READER : DataSourceType.WRITER;

        if (log.isDebugEnabled()) {
            log.debug("Routing datasource -> {} (readOnly={})", target, readOnly);
        }

        return target;
    }
}