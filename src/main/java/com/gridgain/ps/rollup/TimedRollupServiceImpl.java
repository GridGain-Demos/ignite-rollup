package com.gridgain.ps.rollup;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

public class TimedRollupServiceImpl implements Service, RollupService {
    @IgniteInstanceResource
    private Ignite ignite;
    private Timer job;

    private IgniteCache<RollupKey, RollupValue> sourceCache;
    private IgniteCache<RollupKey, RollupValue> destCache;

    private final long rollupNumber;

    public TimedRollupServiceImpl(long rollupNumber) {
        this.rollupNumber = rollupNumber;
    }

    @Override
    public void cancel(ServiceContext ctx) {
        job.cancel();
    }

    @Override
    public void init(ServiceContext ctx) throws Exception {
        sourceCache = ignite.cache("BASE");
        destCache = ignite.cache("ROLLUP");
        job = new Timer();
    }

    @Override
    public void execute(ServiceContext ctx) throws Exception {
        job.schedule(new DoRollup(), 0, 1000);
    }

    private class DoRollup extends TimerTask {
        @Override
        public void run() {
            var query = new SqlFieldsQuery("select b.foreignKey, sum(b.value) from RollupValue b group by b.foreignKey")
                    .setLocal(true);
            try (var cursor = sourceCache.query(query)) {
                for (var r : cursor) {
                    Long id = (Long) r.get(0);
                    BigDecimal value = (BigDecimal) r.get(1);
                    // TODO: check if value needs updating
                    destCache.put(new RollupKey(id, rollupNumber), new RollupValue(value.longValue()));
                }
            }
        }
    }
}
