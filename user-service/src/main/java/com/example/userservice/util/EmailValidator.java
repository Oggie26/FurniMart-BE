package com.example.userservice.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final String EMAIL_PATTERN =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null) return false;
        return email.matches(EMAIL_PATTERN);
    }

}