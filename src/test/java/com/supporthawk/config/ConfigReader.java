package com.supporthawk.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Reads values from config.properties in src/test/resources.
 * The file is loaded once when this class is first used.
 */
public final class ConfigReader {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (inputStream == null) {
                throw new RuntimeException("Could not find config.properties in src/test/resources");
            }

            PROPERTIES.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.properties: " + e.getMessage(), e);
        }
    }

    private ConfigReader() {
    }

    /**
     * Returns the value for a key from config.properties.
     *
     * @param key property name (for example: base.url)
     * @return property value
     */
    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Missing required config key: " + key);
        }
        return value.trim();
    }
}
