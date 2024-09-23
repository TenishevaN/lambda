package com.task11.dto;

import org.json.JSONObject;

public record SignUp(String firstName, String lastName, String email, String password) {

    public SignUp {
        if (email == null || password == null || firstName == null || lastName == null) {
            throw new IllegalArgumentException("Missing or incomplete data.");
        }
    }

    public static SignUp fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        String firstName = json.optString("firstName", null);
        String lastName = json.optString("lastName", null);
        String email = json.optString("email", null);
        String password = json.optString("password", null);


        return new SignUp(firstName, lastName, email, password);
    }

}
