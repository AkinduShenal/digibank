package com.digibank.validation.beneficiary;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = BeneficiaryRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBeneficiaryRequest {

	String message() default "Beneficiary details are invalid.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
