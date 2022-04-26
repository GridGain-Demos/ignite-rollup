package com.gridgain.demo.rollup;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.lang.IgniteAsyncCallback;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;

public class EventRollupServiceImpl implements RollupService, Service {
    @IgniteInstanceResource
    Ignite ignite;
    @LoggerResource
    IgniteLogger log;

    private IgniteCache<RollupKey,RollupValue> sourceCache;
    private IgniteCache<RollupKey,RollupValue> destCache;

    private ContinuousQuery<RollupKey,RollupValue> listener;
    private QueryCursor<Cache.Entry<RollupKey,RollupValue>> cursor;

    private final long rollupNumber;

    public EventRollupServiceImpl(long rollupNumber) {
        this.rollupNumber = rollupNumber;
    }

    @Override
    public void cancel(ServiceContext serviceContext) {
        log.info("Cancelling service " + serviceContext.name());
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        log.info("Initialising service " + serviceContext.name());
        sourceCache = ignite.cache("BASE");
        destCache = ignite.cache("ROLLUP");
        listener = new ContinuousQuery<>();
        listener.setLocal(true);
        listener.setLocalListener(new RollupKeyRollupValueCacheEntryUpdatedListener());
    }

    @Override
    public void execute(ServiceContext serviceContext) throws Exception {
        log.info("Executing service " + serviceContext.name());
        cursor = sourceCache.query(listener);
    }

    @IgniteAsyncCallback
    private class RollupKeyRollupValueCacheEntryUpdatedListener implements CacheEntryUpdatedListener<RollupKey, RollupValue> {
        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends RollupKey, ? extends RollupValue>> cacheEntryEvents) throws CacheEntryListenerException {
            for (var r : cacheEntryEvents) {
                var key = new RollupKey(r.getKey().getForeignKey(), rollupNumber);
                var l = destCache.invoke(key, (CacheEntryProcessor<RollupKey, RollupValue, Long>) (entry, arguments) -> {
                    Long val = null;
                    switch (r.getEventType()) {
                        case CREATED:
                            val = r.getValue().getValue();
                            break;
                        case UPDATED:
                            val = r.getValue().getValue() - r.getOldValue().getValue();
                            break;
                        case REMOVED:
                            val = -r.getOldValue().getValue();
                            break;
                    }
                    Long oldVal = 0L;
                    if (entry.exists()) {
                        oldVal = entry.getValue().getValue();
                    }
                    if (val != null) {
                        entry.setValue(new RollupValue(oldVal + val));
                    }
                    return val;
                });
            }
        }
    }
}
