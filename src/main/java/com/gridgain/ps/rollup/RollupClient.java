package com.gridgain.ps.rollup;

import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;

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

            ignite.services().cancel("Rollup 1");
            ignite.services().cancel("Rollup 2");
            ignite.services().deployNodeSingleton("Rollup 1", new EventRollupServiceImpl(1));
            var rollup2 = new TimedRollupServiceImpl(2, 1_000);
            ignite.services().deployNodeSingleton("Rollup 2", rollup2);
        }
    }
}
