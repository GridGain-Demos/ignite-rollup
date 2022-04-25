package com.gridgain.ps.rollup;

import org.apache.ignite.Ignition;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.Collections;

public class RollupClient {
    public static void main(String[] args) {
        try (var ignite = Ignition.start(RollupConfiguration.getConfiguration().setClientMode(true))) {
            var baseTableConfiguration = new CacheConfiguration<RollupKey,RollupValue>()
                    .setName("BASE")
                    .setBackups(1)
                    .setIndexedTypes(RollupKey.class, RollupValue.class);
            var baseCache = ignite.getOrCreateCache(baseTableConfiguration);

            var rollupTableConfiguration = new CacheConfiguration<Long,RollupValue>()
                    .setName("ROLLUP")
                    .setBackups(1)
                    .setQueryEntities(Collections.singleton(
                            new QueryEntity(Long.class, RollupValue.class)
                                    .addQueryField("id", Long.class.getName(), null)
                                    .addQueryField("value", Long.class.getName(), null)
                                    .setKeyFieldName("id")
                    ));
            var rollupCache = ignite.getOrCreateCache(rollupTableConfiguration);

            ignite.services().deployNodeSingleton("Rollup", new RollupServiceImpl());
        }
    }
}
