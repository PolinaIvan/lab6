package commands;

import model.Worker;

/**
 * Интерфейс для всех команд приложения.
 * Каждая команда должна реализовывать метод execute, который выполняет соответствующее действие.
 */
public interface Command {
    void execute(String argument);

    // по умолчанию игнорируем worker и вызываем старый метод
    // так команды без Worker ничего не меняют
    default void execute(String argument, Worker worker) {
        execute(argument);
    }
}