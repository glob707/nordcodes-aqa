package ru.nordcodes.aqa.fixtures;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Тестовые токены.
 * Хранилище токенов в приложении общее на весь прогон, поэтому каждый тест
 * берёт новый уникальный токен — так тесты не влияют друг на друга.
 */
public final class Tokens {

    /** Длина токена по документации. */
    public static final int LENGTH = 32;

    private Tokens() {
    }

    /**
     * Уникальный корректный токен из цифр и букв A–F.
     * Документация допускает A–Z, но приложение принимает только A–F (дефект D-01),
     * поэтому позитивные сценарии используют символы, допустимые в обоих случаях.
     */
    public static String random() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    /** Случайный токен длины 32 из указанного набора символов. */
    public static String random(String alphabet) {
        StringBuilder token = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            token.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }
        return token.toString();
    }

    /** Токен из допустимых символов заданной длины. */
    public static String ofLength(int length) {
        StringBuilder token = new StringBuilder();
        while (token.length() < length) {
            token.append(random());
        }
        return token.substring(0, length);
    }

    /** Корректный по длине токен, в котором последний символ заменён на указанный. */
    public static String withLastChar(char lastChar) {
        return random().substring(0, LENGTH - 1) + lastChar;
    }
}
