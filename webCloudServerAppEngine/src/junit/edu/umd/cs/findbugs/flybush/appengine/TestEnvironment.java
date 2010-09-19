package edu.umd.cs.findbugs.flybush.appengine;

import com.google.apphosting.api.ApiProxy;

class TestEnvironment implements ApiProxy.Environment {
    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAppId() {
        return "test";
    }

    public String getVersionId() {
        return "1.0";
    }

    public String getEmail() {
        return email;
    }

    public boolean isLoggedIn() {
        return email != null;
    }

    public boolean isAdmin() {
        throw new UnsupportedOperationException();
    }

    public String getAuthDomain() {
        return email == null ? null : "domain.com";
    }

    public String getRequestNamespace() {
        return "";
    }

    public Map<String, Object> getAttributes() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("com.google.appengine.server_url_key", "http://localhost:8080");
        return map;
    }
}
