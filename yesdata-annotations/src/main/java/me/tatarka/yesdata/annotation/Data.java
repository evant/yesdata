package me.tatarka.yesdata.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Denote a data class. That is, one where {@code equals}, {@code hashCode}, and {@code toString} is
 * wholly dependent on the value of it's fields.
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface Data {
}
