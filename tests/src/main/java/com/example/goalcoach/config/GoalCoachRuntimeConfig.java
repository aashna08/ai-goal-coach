package com.example.goalcoach.config;

import java.io.InputStream;
import java.util.Properties;

public final class GoalCoachRuntimeConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream input = GoalCoachRuntimeConfig.class
                .getClassLoader()
                .getResourceAsStream("goalcoach.properties")) {

            if (input != null) {
                props.load(input);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load config file", e);
        }
    }

    private GoalCoachRuntimeConfig() {
        // prevent instantiation
    }

    /**
     * Priority:
     * 1. System Property
     * 2. Environment Variable
     * 3. Properties file
     * 4. Default value
     */
    public static String resolve(String propertyKey, String envVarName, String fallbackDefault) {

        // 1️⃣ System property
        String value = System.getProperty(propertyKey);
        if (isValid(value)) {
            return value.trim();
        }

        // 2️⃣ Environment variable
        if (envVarName != null) {
            String env = System.getenv(envVarName);
            if (isValid(env)) {
                return env.trim();
            }
        }

        // 3️⃣ Properties file
        value = props.getProperty(propertyKey);
        if (isValid(value)) {
            return value.trim();
        }

        // 4️⃣ Default value
        return fallbackDefault == null ? "" : fallbackDefault;
    }

    private static boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }
}