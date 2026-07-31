package com.digibank.validation.validator;

import com.digibank.validation.annotation.Adult;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {

	private static final int MINIMUM_AGE = 18;

	@Override
	public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
		if (dateOfBirth == null) {
			return true;
		}
		return Period.between(dateOfBirth, LocalDate.now()).getYears() >= MINIMUM_AGE;
	}
}
