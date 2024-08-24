package net.foxgenesis.springJDA.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.validator.constraints.Range;

import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraintvalidation.SupportedValidationTarget;
import jakarta.validation.constraintvalidation.ValidationTarget;

@Documented
@Constraint(validatedBy = { })
@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)

@Range
@ReportAsSingleViolation
public @interface Snowflake {
	
	@OverridesAttribute(constraint = Range.class, name = "min") long min() default 10000000000000000L;

	@OverridesAttribute(constraint = Range.class, name = "max") long max() default 9223372036854775807L;

	String message() default "{org.hibernate.validator.constraints.Range.message}";
	
	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };
}
