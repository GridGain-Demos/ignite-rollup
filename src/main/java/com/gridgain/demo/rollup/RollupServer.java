package com.gridgain.demo.rollup;

import org.apache.ignite.Ignition;

public class RollupServer {
    public static void main(String[] args) {
        Ignition.start(RollupConfiguration.getConfiguration());
    }
}
