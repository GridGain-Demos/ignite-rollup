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

            var rollupTableConfiguration = new CacheConfiguration<RollupKey,RollupValue>()
                    .setName("ROLLUP")
                    .setBackups(1)
                    .setIndexedTypes(RollupKey.class, RollupValue.class);
            var rollupCache = ignite.getOrCreateCache(rollupTableConfiguration);

            ignite.services().deployNodeSingleton("Rollup 1", new RollupServiceImpl(1));
            ignite.services().deployNodeSingleton("Rollup 2", new TimedRollupServiceImpl(2));
        }
    }
}
