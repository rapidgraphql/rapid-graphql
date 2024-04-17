package org.rapidgraphql.dataloaders;

import org.dataloader.DataLoaderRegistry;

public interface DataLoaderRegistrar {
    void registerIn(DataLoaderRegistry dataLoaderRegistry);
}
