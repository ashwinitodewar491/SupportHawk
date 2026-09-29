package com.supporthawk.config;

/**
 * Centralized path definitions for Josh and Fintech tenants.
 */
public final class TenantRoutes {

    public enum Tenant {
        JOSH,
        FINTECH
    }

    public static final String JOSH_LOGIN = "/login";
    public static final String JOSH_QUERY = "/query";

    public static final String FINTECH_LOGIN = "/fintech/login";
    public static final String FINTECH_QUERY = "/fintech/query";

    private TenantRoutes() {
    }

    public static String loginPath(Tenant tenant) {
        return switch (tenant) {
            case JOSH -> JOSH_LOGIN;
            case FINTECH -> FINTECH_LOGIN;
        };
    }

    public static String queryPath(Tenant tenant) {
        return switch (tenant) {
            case JOSH -> JOSH_QUERY;
            case FINTECH -> FINTECH_QUERY;
        };
    }
}
