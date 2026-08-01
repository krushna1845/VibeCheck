package com.krushna.moviebooking.auth.exception;

public class PhoneNumberAlreadyExistsException extends RuntimeException {
    public PhoneNumberAlreadyExistsException(String phone) {
        super("User with phone number '" + phone + "' already exists");
    }
}
