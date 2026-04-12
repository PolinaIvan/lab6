package common;

import java.io.Serializable;
import model.Worker;

/**
 * Запрос от клиента к серверу.
 * Содержит имя команды, аргумент и (для команд insert/update) объект работника.
 */
public class CommandRequest implements Serializable {
    //уникальный ID версии для сериализации
    private static final long serialVersionUID = 1L;

    //имя команды и аргумент
    private String commandName;
    private String argument;

    //объект работника — используется в командах insert и update
    private Worker worker;

    private String fieldName;      // имя поля, которое запрашивает сервер
    private String fieldValue;

    /**
     * Конструктор для команд без работника (help, show, remove_key и т.д.)
     * @param commandName имя команды
     * @param argument аргумент (может быть пустым)
     */
    public CommandRequest(String commandName, String argument) {
        this.commandName = commandName;
        this.argument = argument;
        this.worker = null;
    }

    /**
     * Конструктор для команд с работником (insert, update)
     * @param commandName имя команды
     * @param worker объект работника
     */
    public CommandRequest(String commandName, Worker worker) {
        this.commandName = commandName;
        this.argument = null;
        this.worker = worker;
    }

    public CommandRequest(String commandName, String argument, Worker worker) {
        this.commandName = commandName;
        this.argument = argument;
        this.worker = worker;
    }

    //геттеры
    public String getCommandName() { return commandName; }
    public String getArgument() { return argument; }
    public Worker getWorker() { return worker; }
}