package com.digibank.validation.annotation;

import com.digibank.validation.validator.TransactionPinValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TransactionPinValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTransactionPin {

	String message() default "Transaction PIN is invalid.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
