
package com.demoapigatewaycognito.dto;

import org.json.JSONObject;

public class SignIn {

    private String email;
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public SignIn(String email, String password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Missing or incomplete data.");
        }
        this.email = email;
        this.password = password;
    }

    public String email() {
        return email;
    }


    public String password() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static SignIn fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        String email = json.optString("email", null);
        String password = json.optString("password", null);

        return new SignIn(email, password);
    }
}