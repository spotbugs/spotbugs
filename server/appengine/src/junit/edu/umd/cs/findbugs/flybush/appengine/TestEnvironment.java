package edu.umd.cs.findbugs.flybush.appengine;

import java.util.HashMap;
import java.util.Map;

import com.google.apphosting.api.ApiProxy;

class TestEnvironment implements ApiProxy.Environment {
    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getAppId() {
        return "test";
    }

    @Override
    public String getVersionId() {
        return "1.0";
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public boolean isLoggedIn() {
        return email != null;
    }

    @Override
    public boolean isAdmin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAuthDomain() {
        return email == null ? null : "domain.com";
    }

    @Override
    public String getRequestNamespace() {
        return "";
    }

    @Override
    public Map<String, Object> getAttributes() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("com.google.appengine.server_url_key", "http://localhost:8080");
        return map;
    }

    @Override
    public long getRemainingMillis() {
       return 5000;
    }
}
