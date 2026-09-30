package io.swagger.v3.oas.annotations.headers;

import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Header {
    String name() default "";

    String description() default "";

    Schema schema() default @Schema();

    boolean required() default false;

    boolean deprecated() default false;

    String ref() default "";

    Explode explode() default Explode.DEFAULT;

    boolean hidden() default false;

    String example() default "";

    ExampleObject[] examples() default {};

    ArraySchema array() default @ArraySchema();
}
