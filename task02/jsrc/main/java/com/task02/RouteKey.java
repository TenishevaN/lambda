package com.task02;

import java.util.Objects;

public class RouteKey {
    private String method;
    private String path;

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public RouteKey(String method, String path) {
        this.method = method;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteKey routeKey = (RouteKey) o;
        return Objects.equals(method, routeKey.method) && Objects.equals(path, routeKey.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path);
    }
}
