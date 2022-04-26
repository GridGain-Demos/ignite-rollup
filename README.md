# Ignite Rollup

## Overview

A very common pattern that we see with many clients, is that data is
fed into an Apache Ignite or GridGain cluster into two tables: one is
the full data, the second is a summary, or rollup, of that data.

With GridGain’s SQL engine, it’s not always necessary to have a summary
table at all! For many, perhaps most, use cases, the fact that the data
is held in memory and distributed efficiently means that even a complex
“group by” expression can be performed very quickly.

However, when the lowest latency is required, performing those aggregations
dynamically is not going to cut it. Here we typically “pre-aggregate” the
data, creating a summary table that can be queried directly.

This is a demo project showing a couple of ways you might implement
that.

## How to run

It's designed to run on your local machine, in an IDE. It'll need
some minor changes to run on "real" servers.

1. Run `RollupServer`. You can run several copies of this. If you add
`-DIGNITE_QUIET=false` you'll see more logging
2. Run `RollupClient`. This will create the tables and deploy the
rollup services
3. Connect using a JDBC client and insert some records
```
0: jdbc:ignite:thin://127.0.0.1> insert into base.rollupvalue values (1,1,1);
1 row affected (0.073 seconds)
0: jdbc:ignite:thin://127.0.0.1> insert into base.rollupvalue values (2,2,1);
1 row affected (0.003 seconds)
0: jdbc:ignite:thin://127.0.0.1> insert into base.rollupvalue values (3,2,2);
1 row affected (0.002 seconds)
0: jdbc:ignite:thin://127.0.0.1> select * from rollup.rollupvalue;
+--------------------------------+--------------------------------+--------------------------------+
|               ID               |           FOREIGNKEY           |             VALUE              |
+--------------------------------+--------------------------------+--------------------------------+
| 2                              | 2                              | 3                              |
| 1                              | 1                              | 1                              |
| 2                              | 1                              | 3                              |
| 1                              | 2                              | 1                              |
+--------------------------------+--------------------------------+--------------------------------+
4 rows selected (0.009 seconds)
```
4. See what happens

## Description

* *EventRollupServiceImpl*. Listens to the base table using a Continuous Query. Updates the
summary table immediately
* *TimedRollupServiceImpl*. Periodically executed a SQL query to generate the summary

See the blog for a more detailed description.