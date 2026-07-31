package com.digibank.validation.annotation;

import com.digibank.validation.validator.InitialDepositValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = InitialDepositValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidInitialDeposit {

	String message() default "Initial deposit does not meet the selected account type minimum.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
