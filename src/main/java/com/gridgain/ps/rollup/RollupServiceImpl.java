package com.gridgain.ps.rollup;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.lang.IgniteAsyncCallback;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.function.Supplier;

public class RollupServiceImpl implements RollupService, Service {
    @IgniteInstanceResource
    Ignite ignite;

    private Supplier<RollupValue> valueSupplier;

    private IgniteCache<RollupKey,RollupValue> sourceCache;
    private IgniteCache<Long,RollupValue> destCache;

//    @FunctionalInterface
//    public interface UpdateRollup<S1,S2> {
//        void updateRollup(EventType e, S1 k, S2 v);
//    }

    private ContinuousQuery<RollupKey,RollupValue> listener;
    private QueryCursor cursor;

    @Override
    public void cancel(ServiceContext serviceContext) {
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        sourceCache = ignite.cache("BASE");
        destCache = ignite.cache("ROLLUP");
        listener = new ContinuousQuery<>();
        listener.setLocal(true);
        listener.setLocalListener(new RollupKeyRollupValueCacheEntryUpdatedListener());
    }

    @Override
    public void execute(ServiceContext serviceContext) throws Exception {
        cursor = sourceCache.query(listener);
    }

    @IgniteAsyncCallback
    private class RollupKeyRollupValueCacheEntryUpdatedListener implements CacheEntryUpdatedListener<RollupKey, RollupValue> {
        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends RollupKey, ? extends RollupValue>> cacheEntryEvents) throws CacheEntryListenerException {
            for (var r : cacheEntryEvents) {
                var l = destCache.invoke(r.getKey().getForeignKey(), (CacheEntryProcessor<Long, RollupValue, Long>) (entry, arguments) -> {
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
                        destCache.put(r.getKey().getForeignKey(), new RollupValue(oldVal + val));
                    }
                    return val;
                });
            }
        }
    }
}
