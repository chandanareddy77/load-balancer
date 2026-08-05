package com.loadbalancer;

import java.util.List;

public interface LoadBalancerStrategy {
    String getServer(List<String> servers);
}