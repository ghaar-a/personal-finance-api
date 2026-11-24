package br.com.tr.personal_finance_api.security;

public final class CsrfDemoCookie {

    public static final String NAME = "CSRF_DEMO_AUTH";
    public static final String PATH = "/api/csrf";

    private CsrfDemoCookie() {
    }
}