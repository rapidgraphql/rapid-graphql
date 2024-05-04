package org.rapidgraphql.dataloaders;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.dataloader.registries.ScheduledDataLoaderRegistry;

public interface DataLoaderRegistrar<K, T> {
    DataLoader<K, T> registerIn(DataLoaderRegistry dataLoaderRegistry);
    String getDataLoaderName();

    /**
     * Indicates if registrar should be used to register dataloader lazily on the first get() entity request
     * @return true - if lazy registration is required, false if dataloader should be registered,
     * when dataloader registry is created
     */
    default boolean isLazyRegistrar() {
        return false;
    }

    /**
     * Indicates if registrar registers scheduled dataloader which requires {@link ScheduledDataLoaderRegistry}
     * @return true - if the DataLoader is Scheduled, false otherwise
     */
    default boolean isScheduled() {
        return false;
    }
}
