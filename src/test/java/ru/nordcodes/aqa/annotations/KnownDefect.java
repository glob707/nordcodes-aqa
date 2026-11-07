package ru.nordcodes.aqa.annotations;

import io.qameta.allure.LabelAnnotation;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Тест проверяет поведение, по которому заведён дефект (список — в README, раздел «Найденные дефекты»).
 * Ожидаемый результат в тесте не подгоняется под фактический: тест падает, пока дефект не исправлен.
 * В основной прогон тесты с этой аннотацией не входят — они запускаются отдельно: mvn test -P defects.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Tag("defect")
@LabelAnnotation(name = "tag")
public @interface KnownDefect {

    /** Идентификатор дефекта, например «Дефект D-01». */
    String value();
}
