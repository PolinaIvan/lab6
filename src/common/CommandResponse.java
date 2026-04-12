package common;

import java.io.Serializable;

/**
 * Ответ от сервера клиенту.
 * Содержит текст для вывода и флаг успешности.
 */
public class CommandResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    //текст, который клиент выведет пользователю
    private String message;

    // true — команда выполнилась успешно, false — была ошибка
    private boolean success;

    /**
     * Конструктор ответа
     * @param message текст для вывода
     * @param success успешность выполнения
     */
    public CommandResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    //геттеры
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
}