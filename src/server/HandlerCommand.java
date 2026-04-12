package server;

import collection.CollectionManager;
import commands.CommandExecutor;
import commands.Command;
import model.Worker;
import utils.InputHelper;
import common.CommandRequest;
import common.CommandResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import validation.ValidatingWorker;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Обработчик команд на сервере.
 * Принимает запрос от клиента, выполняет команду через CommandExecutor,
 * захватывает вывод и возвращает результат клиенту.
 */
public class HandlerCommand {

    private CollectionManager collectionManager;   // управление коллекцией
    private CommandExecutor commandExecutor;       // выполнение команд
    private InputHelper inputHelper;               // ввод данных (для команд insert и т.д.)
    private Scanner scanner;                       // для ввода с консоли сервера
    private ZonedDateTime initDate;                // дата запуска
    private String fileName;                       // имя файла с коллекцией
    private static final Logger logger
            = LoggerFactory.getLogger(HandlerCommand.class); // создаём логгер для этого класса

    /**
     * Конструктор. Создаёт все необходимые объекты и загружает коллекцию.
     *
     * @param fileName имя файла для сохранения/загрузки коллекции
     */
    public HandlerCommand(String fileName) {
        this.fileName = fileName;

        // Создаём вспомогательные объекты
        this.scanner = new Scanner(System.in);
        this.inputHelper = new InputHelper(scanner);
        this.collectionManager = new CollectionManager();
        this.initDate = ZonedDateTime.now();

        // Создаём исполнитель команд
        this.commandExecutor = new CommandExecutor(
                collectionManager, inputHelper, scanner, initDate, fileName
        );

        // Загружаем коллекцию из файла
        ValidatingWorker validator = new ValidatingWorker(() -> {
            CollectionManager temp = new CollectionManager();
            commandExecutor.loadFromFile(temp);  // или другой метод загрузки
            return temp.getCollection();
        });

        TreeMap<Long, Worker> loaded = validator.load();

        for (var entry : loaded.entrySet()) {
            collectionManager.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Выполняет команду, полученную от клиента.
     *
     * @param request запрос от клиента (имя команды и аргумент)
     * @return ответ для клиента (результат выполнения команды)
     */
    public CommandResponse executeCommand(CommandRequest request) {
        // достаём имя команды и аргумент из запроса клиента
        String commandName = request.getCommandName();
        String argument = request.getArgument();
        Worker worker = request.getWorker();

        logger.info("Начало обработки: {}", commandName);

        try {
            // создаём буфер для захвата вывода команды
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // оборачиваем буфер в PrintStream, чтобы команды могли в него писать
            PrintStream ps = new PrintStream(baos);
            // запоминаем старый вывод из консоли, чтобы потом вернуть
            PrintStream oldOut = System.out;

            // Ищем команду по имени в CommandExecutor
            Command command = commandExecutor.getCommand(commandName);

            if (command != null){
                logger.info("Приступаю к выполнению команды: {}...", commandName);
            }

            // временно подменяем System.out на наш буфер
            // теперь всё, что команда напечатает, попадёт в baos
            System.setOut(ps);

            if (worker != null && command != null) {
                // для команд, которым нужен worker (insert, update_id)
                command.execute(argument, worker);
            } else if (command != null) {
                // выполняем команду с аргументом
                command.execute(argument);
            } else {
                // Если команда не найдена, пишем ошибку в буфер
                System.out.println("Неизвестная команда: " + commandName);
            }

            // принудительно сбрасываем буфер (выталкиваем из PrintStream в ByteArrayOutputStream)
            System.out.flush();

            // забираем результат из ByteArrayOutputStream
            String result = baos.toString();

            // возвращаем вывод обратно в консоль
            System.setOut(oldOut);

            if (command != null) {
                logger.info("Команда {} выполнена", commandName);
            }

            // возвращаем ответ клиенту с результатом и флагом успеха
            return new CommandResponse(result, true);

        } catch (Exception e) {
            // если произошла ошибка — выводим её в лог сервера
            logger.error("ОШИБКА при выполнении команды {}: {}", commandName, e.getMessage(), e);
            // возвращаем ответ с текстом ошибки и флагом false
            return new CommandResponse("Ошибка: " + e.getMessage(), false);
        }
    }
}
