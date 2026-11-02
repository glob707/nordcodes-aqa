package ru.nordcodes.aqa.models;

/**
 * Тело ответа приложения: {@code {"result": "OK"}} или {@code {"result": "ERROR", "message": "..."}}.
 */
public record ApiResponse(String result, String message) {

    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
}
