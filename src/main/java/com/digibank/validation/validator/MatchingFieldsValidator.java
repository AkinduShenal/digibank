package com.digibank.validation.validator;

import com.digibank.validation.annotation.MatchingFields;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Objects;

public class MatchingFieldsValidator implements ConstraintValidator<MatchingFields, Object> {

	private String firstFieldName;
	private String secondFieldName;
	private String message;

	@Override
	public void initialize(MatchingFields constraintAnnotation) {
		this.firstFieldName = constraintAnnotation.first();
		this.secondFieldName = constraintAnnotation.second();
		this.message = constraintAnnotation.message();
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		Object firstValue = readField(value, firstFieldName);
		Object secondValue = readField(value, secondFieldName);
		boolean valid = Objects.equals(firstValue, secondValue);
		if (!valid) {
			addFieldMessage(context, secondFieldName, message);
		}
		return valid;
	}

	private Object readField(Object target, String fieldName) {
		try {
			Field field = findField(target.getClass(), fieldName);
			field.setAccessible(true);
			return field.get(target);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Could not read field: " + fieldName, ex);
		}
	}

	private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null) {
			try {
				return current.getDeclaredField(fieldName);
			}
			catch (NoSuchFieldException ex) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}

	private void addFieldMessage(ConstraintValidatorContext context, String fieldName, String fieldMessage) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(fieldMessage)
				.addPropertyNode(fieldName)
				.addConstraintViolation();
	}
}
