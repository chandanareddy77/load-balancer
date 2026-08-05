package com.loadbalancer;

import java.util.List;
import java.util.Random;

public class RandomStrategy implements LoadBalancerStrategy {
    private final Random random = new Random();

    @Override
    public String getServer(List<String> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        int randomIndex = random.nextInt(servers.size());
        return servers.get(randomIndex);
    }
}