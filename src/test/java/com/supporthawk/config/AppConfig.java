package com.supporthawk.config;

/** UI base URL + run config. Never hardcode a host in a page object or test — add it here. */
public final class AppConfig {

    private AppConfig() {
    }

    public static final String BASE_URL = ConfigReader.get("base.url");

    public static boolean isHeadless() {
        return Boolean.parseBoolean(System.getProperty("headless", "false"));
    }
}
