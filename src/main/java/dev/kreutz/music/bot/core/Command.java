package dev.kreutz.music.bot.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String name() default "";

    String description();

    long[] enabledUsers() default {};

    long[] disabledUsers() default {};

    long[] enabledRoles() default {};

    long[] disabledRoles() default {};
}
