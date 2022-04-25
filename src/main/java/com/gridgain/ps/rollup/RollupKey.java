package com.gridgain.ps.rollup;

import lombok.Value;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

@Value
public class RollupKey {
    @QuerySqlField
    private long id;
    @QuerySqlField
    @AffinityKeyMapped
    private long foreignKey;
}
