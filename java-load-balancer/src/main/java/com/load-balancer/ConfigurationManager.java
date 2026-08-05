package com.loadbalancer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConfigurationManager {
    private final Properties properties = new Properties();

    public ConfigurationManager() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties");
            }
            properties.load(input);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load configuration file", ex);
        }
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("server.port", "8080"));
    }

    public String getAlgorithm() {
        return properties.getProperty("lb.algorithm", "round-robin").trim().toLowerCase();
    }

    public List<String> getBackendServers() {
        String servers = properties.getProperty("backend.servers", "");
        return Arrays.asList(servers.split(","));
    }
}