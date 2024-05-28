package com.task02;

import java.util.Objects;

public class Body {
    private String message;
    private String error;

    static Body ok(String message) {
        return new Body(message, null);
    }

    static Body error(String error) {
        return new Body(null, error);
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public Body(String message, String error) {
        this.message = message;
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Body body = (Body) o;
        return Objects.equals(message, body.message) && Objects.equals(error, body.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, error);
    }
}
