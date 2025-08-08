package com.astro.shared.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
@Pattern(regexp = "[a-z0-9]{3,}", message = "Slug must be at least 3 characters long and contain only lowercase letters and numbers.")
public @interface ValidSlug {
    String message() default "Invalid slug format.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}