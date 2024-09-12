
package com.demoapigatewaycognito.dto;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignUp {
    private String email;
    private String password;
    private String firstName;
    private String lastName;

    private static final Logger logger = LoggerFactory.getLogger(SignUp.class);

    public SignUp(String email, String password, String firstName, String lastName) {
        logger.info(email);
        logger.info(password);
        logger.info(firstName);
        logger.info(lastName);
        if (email == null || password == null || firstName == null || lastName == null) {
            logger.info("TMissing or incomplete data");
            throw new IllegalArgumentException("Missing or incomplete data.");
        }
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String email() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String password() {
        return password;
    }

    public String firstName() {
        return firstName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String lastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    public static SignUp fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        String email = json.optString("email", null);
        String password = json.optString("password", null);
        String firstName = json.optString("firstName", null);
        String lastName = json.optString("lastName", null);
        logger.info("Sign up");
        return new SignUp(email, password, firstName, lastName);
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SignUp{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}