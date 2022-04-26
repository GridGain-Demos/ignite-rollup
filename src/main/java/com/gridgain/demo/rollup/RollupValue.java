package com.gridgain.demo.rollup;

import lombok.Value;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

@Value
public class RollupValue {
    @QuerySqlField
    private long value;
}
