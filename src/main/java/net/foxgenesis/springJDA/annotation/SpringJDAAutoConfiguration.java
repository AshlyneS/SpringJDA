package net.foxgenesis.springJDA.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import net.foxgenesis.springJDA.SpringJDA;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Documented

@ConditionalOnClass(SpringJDA.class)
@AutoConfiguration(before = net.foxgenesis.springJDA.autoconfigure.SpringJDAAutoConfiguration.class)
public @interface SpringJDAAutoConfiguration {
}
