package com.gridgain.ps.rollup;

import lombok.Value;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

@Value
public class RollupValue {
    @QuerySqlField
    private long value;
}
