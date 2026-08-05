package com.loadbalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements LoadBalancerStrategy {
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public String getServer(List<String> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        int currentIndex = Math.abs(index.getAndIncrement() % servers.size());
        return servers.get(currentIndex);
    }
}