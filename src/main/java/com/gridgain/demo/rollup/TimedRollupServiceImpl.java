package com.gridgain.demo.rollup;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

public class TimedRollupServiceImpl implements Service, RollupService {
    @IgniteInstanceResource
    private Ignite ignite;
    @LoggerResource
    IgniteLogger log;

    long rollupFrequency = 1000;
    private Timer job;

    private IgniteCache<RollupKey, RollupValue> sourceCache;
    private IgniteCache<RollupKey, RollupValue> destCache;

    private final long rollupNumber;

    public TimedRollupServiceImpl(long rollupNumber, long rollupFrequency) {
        this.rollupNumber = rollupNumber;
        this.rollupFrequency = rollupFrequency;
    }

    public long getRollupFrequency() {
        return rollupFrequency;
    }

    @Override
    public void cancel(ServiceContext ctx) {
        log.info("Cancelling service " + ctx.name());
        job.cancel();
    }

    @Override
    public void init(ServiceContext ctx) throws Exception {
        log.info("Initialising service " + ctx.name());
        sourceCache = ignite.cache("BASE");
        destCache = ignite.cache("ROLLUP");
        job = new Timer();
    }

    @Override
    public void execute(ServiceContext ctx) throws Exception {
        log.info("Executing service " + ctx.name());
        job.schedule(new DoRollup(ctx.name()), 0, getRollupFrequency());
    }

    private class DoRollup extends TimerTask {
        private String serviceName;

        public DoRollup(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }

        @Override
        public void run() {
            log.info("Run rollup service " + getServiceName());
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
