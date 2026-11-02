package ru.nordcodes.aqa.annotations;

import io.qameta.allure.LabelAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Документация не задаёт ожидаемый результат; тест фиксирует поведение,
 * выявленное при исследовании приложения (регрессионная проверка).
 * Отображается в Allure как тег.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@LabelAnnotation(name = "tag", value = "Фактическое поведение приложения")
public @interface BasedOnObservedBehavior {
}
