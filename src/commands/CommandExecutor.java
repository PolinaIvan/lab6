package commands;

import collection.CollectionManager;
import utils.InputHelper;
import model.*;

import java.time.LocalDate;
import java.util.*;
import java.time.ZonedDateTime;

import java.io.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс для хранения всех команд приложения.
 * Содержит все команды приложения, каждая из которых
 * реализована как анонимный класс с методом execute.
 * Команды хранятся в HashMap для быстрого доступа по имени.
 */
public class CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
    private CollectionManager collectionManager;
    private InputHelper inputHelper;
    private Scanner scanner;
    private ZonedDateTime initDate;
    private String fileName;        // имя файла для сохранения
    private Gson gson;              // для работы с JSON

    // Карта команд: ключ - название команды (String) и значение - объект, реализующий интерфейс (Command)
    private Map<String, Command> commands;

    /**
     * Конструктор класса CommandExecutor.
     *
     * @param collectionManager менеджер коллекции
     * @param inputHelper помощник для ввода
     * @param scanner сканер для чтения
     * @param initDate для хранения даты запуска
     * @param fileName имя файла
     */
    public CommandExecutor(CollectionManager collectionManager, InputHelper inputHelper,
                           Scanner scanner, ZonedDateTime initDate, String fileName) {
        this.collectionManager = collectionManager;
        this.inputHelper = inputHelper;
        this.scanner = scanner;
        this.initDate = initDate;
        this.fileName = fileName;
        this.commands = new HashMap<>();
        initCommands();  // заполняем карту командами
    }

    /**
     * Инициализирует все команды приложения
     * Каждая команда добавляется в commands
     */
    private void initCommands() {

        // команда help
        commands.put("help", new Command() {
            /**
             * Выводит список всех доступных команд
             *
             * @param argument не используется, но должен быть по контракту интерфейса
             */
            @Override
            public void execute(String argument) {
                System.out.println("\n___ Доступные команды ___");
                System.out.println("help  - показать справку");
                System.out.println("info  - информация о коллекции");
                System.out.println("show  - показать всех работников");
                System.out.println("insert ключ     - добавить нового работника с заданным ключом");
                System.out.println("remove_key ключ    - удалить работника с ключом");
                System.out.println("remove_greater_key ключ - удалить все с ключом больше заданного");
                System.out.println("clear  - очистить коллекцию ");
                System.out.println("save  - сохранить коллекцию в файл");
                System.out.println("update_id {id}      - обновить работника по его id");
                System.out.println("remove_lower  - удалить из коллекции все элементы, меньшие, чем заданный");
                System.out.println("sum_of_salary  - сумма зарплат всех работников");
                System.out.println("filter_by_status статус  - показать работников с заданным статусом");
                System.out.println("print_field_descending_status - вывести статусы в порядке убывания");
                System.out.println("replace_if_lower ключ - заменить работника, если новый имеет зарплату меньше");
                System.out.println("execute_script имя_файла - выполнить скрипт из файла");
                System.out.println("exit  - выход");
            }
        });

        // команда info
        commands.put("info", new Command() {
            /**
             * Выводит информацию о коллекции
             */
            @Override
            public void execute(String argument) {
                System.out.println("\n___ Информация о коллекции ___");
                System.out.println("Тип: TreeMap");
                System.out.println("Количество элементов: " + collectionManager.size());
                System.out.println("Дата инициализации: " + initDate);
            }
        });

        // команда show
        commands.put("show", new Command() {
            /**
             * Выводит всех работников в коллекции.
             * Проходит по всем entrySet парам ключ-значение и выводит их
             */
            @Override
            public void execute(String argument) {
                if (collectionManager.isEmpty()) {
                    System.out.println("Коллекция пуста");
                } else {
                    System.out.println("\n___ Все работники ___");
                    // entry.getKey() - ключ
                    // entry.getValue() - работник

                    // сортировка по местоположению (сначала по x, потом по y)
                    collectionManager.getCollection().entrySet().stream()
                            .sorted((e1, e2) -> {
                                Coordinates c1 = e1.getValue().getCoordinates();
                                Coordinates c2 = e2.getValue().getCoordinates();
                                int xCompare = c1.getX().compareTo(c2.getX());
                                if (xCompare != 0) return xCompare;
                                return c1.getY().compareTo(c2.getY());
                            })
                            .forEach(entry -> System.out.println
                                    ("Ключ " + entry.getKey() + ": " + entry.getValue()));
                }
            }
        });

        // команда insert
        commands.put("insert", new Command() {
            /**
             * Добавляет нового работника в коллекцию.
             * Данные работника запрашивает через InputHelper

             * @param argument ключ для нового работника
             */
            @Override
            public void execute(String argument) {
                System.out.println("Ошибка: insert должен вызываться с worker");
            }
            @Override
            public void execute(String argument, Worker worker) {
                try {
                    Long key = Long.parseLong(argument);
                    if (collectionManager.containsKey(key)) {
                        System.out.println("Ключ " + key + " уже существует, попробуйте другой!");
                        return;
                    }

                    // используем worker, пришедший от клиента
                    collectionManager.put(key, worker);
                    System.out.println("Добавлен работник с ключом " + key);

                } catch (NumberFormatException e) {
                    if (argument.isEmpty()) {
                        System.out.println("Пожалуйста, укажите ключ!");
                    } else {
                        System.out.println("Ключ должен быть числом!");
                    }
                }
            }
        });

        // команда сохранения save
        commands.put("save", new Command() {
            @Override
            public void execute(String argument) {
                try {
                    // создаем объект, который превращает текст в байты для файла
                    // OutputStreamWriter связывает между символами и байтами
                    // FileOutputStream открывает файл для записи байтов
                    // "UTF-8" для того, чтобы русские буквы отображались корректно
                    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");

                    // GsonBuilder - строитель для создания Gson с настройками
                    // create() собирает Gson с указанными настройками
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();

                    // toJson() превращает Java-объект (коллекцию) в JSON-строку
                    // writer принимает эту строку и отправляет в файл
                    // collectionManager.getCollection() - берем TreeMap с работниками из менеджера
                    gson.toJson(collectionManager.getCollection(), writer);

                    // закрываем файл, без этого данные могут не сохраниться на диск
                    writer.close();

                    System.out.println("Сохранено" + collectionManager.size() + " работников в " + fileName);

                } catch (IOException e) {
                    // если возникла проблема выводим ошибку
                    System.out.println("Ошибка сохранения" + e.getMessage());
                }
            }
        });

        // команда remove key
        commands.put("remove_key", new Command() {
            /**
             * Удаляет работника по ключу
             *
             * @param argument ключ для удаления
             */
            @Override
            public void execute(String argument) {
                try {
                    // записываем ключ работника
                    Long key = Long.parseLong(argument);
                    if (collectionManager.containsKey(key)) {
                        // удаляем ключ, если нашли работника в коллекции
                        collectionManager.remove(key);
                        System.out.println("Работник с ключом " + key + " удален");
                    } else {
                        System.out.println("Ключ " + key + " не найден");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Ключ должен быть числом! Повторите попытку");
                }
            }
        });

        // команда remove_greater_key
        commands.put("remove_greater_key", new Command() {
            /**
             * Удаляет все элементы с ключом больше заданного.
             * Использует keySet() чтобы получить все ключи, и removeIf() для удаления с условием
             *
             * @param argument пороговый ключ
             */
            @Override
            public void execute(String argument) {
                try {
                    Long key = Long.parseLong(argument);
                    int before = collectionManager.size(); //считаем сколько было до удаления

                    // k -> k > key - для каждого ключа k проверить, больше ли он key
                    collectionManager.getCollection().keySet().removeIf(k -> k > key);

                    int removed = before - collectionManager.size(); //сколько удалили

                    if (removed == 0){
                        System.out.println("Не нашлось ни одного элемента с ключом больше " + key);
                    } else{
                        System.out.println("Удалено " + removed + " элементов с ключом > " + key);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Ключ должен быть числом! Попробуйте снова");
                }
            }
        });

        // команда clear
        commands.put("clear", new Command() {
            /**
             * Очищает всю коллекцию
             */
            @Override
            public void execute(String argument) {
                collectionManager.clear();
                System.out.println("Коллекция очищена");
            }
        });

        // команда exit
        commands.put("exit", new Command() {
            /**
             * Завершает работу программы
             */
            @Override
            public void execute(String argument) {
                System.out.println("До свидания!");
                System.exit(0);  // 0 - успешное завершение
            }
        });

        // команда подсчёта суммы зарплат
        commands.put("sum_of_salary", new Command() {
            /**
             * Вычисляет сумму зарплат всех работников.
             * Проходит по values() (только значения) и суммирует зарплаты
             */
            @Override
            public void execute(String argument) {
                if (collectionManager.isEmpty()) {
                    System.out.println("Коллекция пуста, сумма = 0");
                } else {
                    // собираем все значения работников
                    var workers = collectionManager.getCollection().values();

                    // сумма зарплат
                    // .filter() — опять оставляем только с зарплатой
                    // .mapToDouble() — превращаем Worker → double (зарплату)
                    // .sum() — складываем все зарплаты
                    double total = workers.stream()
                            .filter(w -> w.getSalary() != null)
                            .mapToDouble(Worker::getSalary)
                            .sum();

                    // количество работников с зарплатой
                    long count = workers.stream()
                            .filter(w -> w.getSalary() != null)
                            .count();

                    // выводим результат
                    System.out.println("\nСумма зарплат: " + total);
                    System.out.println("Учитывает работников с зарплатой: " + count + " из " + collectionManager.size());
                }
            }
        });

        // команда фильтра по статусу
        commands.put("filter_by_status", new Command() {
            /**
             * Выводит работников с указанным статусом.
             * Проходит по entrySet и выводит подходящих
             *
             * @param argument статус для фильтрации
             */
            @Override
            public void execute(String argument) {
                if (argument.isEmpty()) {
                    System.out.println("Укажите статус! Пример: filter_by_status HIRED");
                    System.out.println("Доступные статусы: " + java.util.Arrays.toString(Status.values()));
                } else {
                    try {
                        // Status.valueOf() пробует преобразовать строку в enum
                        Status inputStatus = Status.valueOf(argument.toUpperCase());

                        System.out.println("\n___ Работники со статусом " + inputStatus + " ___");

                        long count = collectionManager.getCollection().entrySet().stream()
                                .filter(entry -> entry.getValue().getStatus() == inputStatus)
                                .count();

                        if (count == 0) {
                            System.out.println("Работники со статусом " + inputStatus + " не найдены");
                        } else {
                            System.out.println("Найдено работников: " + count);
                        }

                    } catch (IllegalArgumentException e) {
                        System.out.println("Неверный статус! Доступны: " + java.util.Arrays.toString(Status.values()));
                    }
                }
            }
        });

        // команда вывода статусов в обратном порядке
        commands.put("print_field_descending_status", new Command() {
            /**
             * Выводит статусы всех работников в порядке убывания.
             * Собирает все статусы и сортирует через Comparator.reverseOrder()
             */
            @Override
            public void execute(String argument) {
                if (collectionManager.isEmpty()) {     //проверяем, пустая ли коллекция
                    System.out.println("Коллекция пуста");
                } else {
                    System.out.println("\n___ Статусы всех работников (по убыванию) ___");

                    // берём только статусы (.map(Worker::getStatus))
                    // сортируем в обратном порядке (.sorted(Comparator.reverseOrder()))
                    // собираем в список (.collect(Collectors.toList()))
                    List<Status> statuses = collectionManager.getCollection().values().stream()
                            .map(Worker::getStatus)
                            .sorted(Comparator.reverseOrder())
                            .toList();

                    System.out.println("\n Всего статусов: " + statuses.size()); //пишем сколько всего
                }
            }
        });

        // команда удаления работников меньше заданного
        commands.put("remove_lower", new Command() {
            /**
             * Удаляет работников меньше эталонного.
             * Запрашивает эталонного работника через InputHelper.
             * Использует compareTo для сравнения.
             * Удаляет тех, у кого compareTo < 0
             */
            @Override
            public void execute(String argument) {
                System.out.println("remove_lower должен вызываться с worker");
            }

            @Override
            public void execute(String argument, Worker reference) {
                int before = collectionManager.size();

                collectionManager.getCollection().entrySet().removeIf(
                        entry -> entry.getValue().compareTo(reference) < 0);

                int removed = before - collectionManager.size();
                System.out.println("Удалено " + removed + " работников");
            }
        });

        // команда замены работника, если его зарплата меньше старого
        commands.put("replace_if_lower", new Command() {
            /**
             * Заменяет работника по ключу, если новый просит денег меньше старого.
             * 1. Проверяет ключ
             * 2. Получает старого работника
             * 3. Создает нового через InputHelper
             * 4. Сравнивает по зарплате
             * 5. Если новая меньше - заменяет, сохраняя id и дату
             *
             * @param argument ключ для замены
             */
            @Override
            public void execute(String argument) {
                System.out.println("Ошибка: replace_if_lower должен вызываться с worker");
            }

            @Override
            public void execute(String argument, Worker newWorker) {
                try {
                    Long key = Long.parseLong(argument);
                    if (!collectionManager.containsKey(key)) {
                        System.out.println("Ключ " + key + " не найден");
                        return;
                    }

                    Worker oldWorker = collectionManager.get(key);
                    if (newWorker.getSalary() < oldWorker.getSalary()) {
                        newWorker.setId(oldWorker.getId());
                        newWorker.setCreationDate(oldWorker.getCreationDate());
                        collectionManager.put(key, newWorker);
                        System.out.println("Замена выполнена!");
                    } else {
                        System.out.println("Замена не выполнена. Новая зарплата не меньше старой.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Ключ должен быть числом!");
                }
            }
        });

        // команда update
        commands.put("update_id", new Command() {
            /**
             * Обновляет работника по его id.
             *
             * @param argument должен содержать "id {число}"
             */
            @Override
            public void execute(String argument) {
                //проверяем, указан ли id
                if (argument.isEmpty()) {
                    System.out.println("Укажите id работника! Пример: update id 5");
                    return;
                }

                try {
                    Integer targetId = Integer.parseInt(argument);
                    boolean found = false;
                    Long foundKey = null;
                    Worker foundWorker = null;

                    //ищем работника с таким id
                    for (var entry : collectionManager.getCollection().entrySet()) {
                        if (entry.getValue().getId().equals(targetId)) {
                            found = true;
                            foundKey = entry.getKey();
                            foundWorker = entry.getValue();
                            break;
                        }
                    }

                    if (!found) {
                        System.out.println("Работник с id " + targetId + " не найден");
                        return;
                    }

                    //показываем старого работника
                    System.out.println("\nНайден работник:");
                    System.out.println("Ключ: " + foundKey);
                    System.out.println(foundWorker);

                    //создаем нового работника
                    System.out.println("\n📝 Введите новые данные:");
                    Worker newWorker = inputHelper.readWorkerForComparison();

                    if (newWorker == null) {
                        System.out.println("Ошибка создания работника");
                        return;
                    }

                    //сохраняем id и дату создания старого работника
                    newWorker.setId(foundWorker.getId());
                    newWorker.setCreationDate(foundWorker.getCreationDate());

                    //заменяем в коллекции (по тому же ключу)
                    collectionManager.put(foundKey, newWorker);

                    System.out.println("\n  Работник с id " + targetId + " обновлен!");
                    System.out.println("  Ключ " + foundKey + " сохранен");

                } catch (NumberFormatException e) {
                    System.out.println("Id должен быть числом! Попробуйте снова");
                }
            }
        });

        commands.put("execute_script", new Command() {
            @Override
            public void execute(String argument) {
                if (argument.isEmpty()) {
                    System.out.println("Укажите имя файла!");
                    return;
                }

                File file = new File(argument);
                if (!file.exists()) {
                    System.out.println("Файл не найден");
                    return;
                }

                System.out.println("\nВыполняю скрипт из файла: " + argument);

                try (Scanner fileScanner = new Scanner(file)) {
                    // Буфер для сбора вывода
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    PrintStream oldOut = System.out;
                    System.setOut(ps);

                    int lineNumber = 0;

                    while (fileScanner.hasNextLine()) {
                        lineNumber++;
                        String line = fileScanner.nextLine().trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;

                        String[] parts = line.split(" ", 2);
                        String cmd = parts[0].toLowerCase();
                        String arg = parts.length > 1 ? parts[1] : "";

                        if (cmd.equals("exit") || cmd.equals("execute_script")) {
                            System.out.println("Команда " + cmd + " в скрипте запрещена");
                            continue;
                        }

                        // INSERT
                        if (cmd.equals("insert")) {
                            // Проверка ключа
                            Long key;
                            try {
                                key = Long.parseLong(arg);
                            } catch (NumberFormatException e) {
                                System.out.println("Ошибка: ключ должен быть числом!");
                                continue;
                            }

                            // Проверка существования ключа
                            if (collectionManager.containsKey(key)) {
                                System.out.println("Ключ " + key + " уже существует");
                                // Пропускаем поля (9 строк)
                                for (int i = 0; i < 9; i++) {
                                    if (fileScanner.hasNextLine()) fileScanner.nextLine();
                                }
                                continue;
                            }

                            // Читаем поля из файла
                            String name = fileScanner.nextLine().trim();
                            String xStr = fileScanner.nextLine().trim();
                            String yStr = fileScanner.nextLine().trim();
                            String salaryStr = fileScanner.nextLine().trim();
                            String positionStr = fileScanner.nextLine().trim();
                            String statusStr = fileScanner.nextLine().trim();
                            String heightStr = fileScanner.nextLine().trim();
                            String weightStr = fileScanner.nextLine().trim();
                            String birthdayStr = fileScanner.nextLine().trim();

                            // Создаём Worker
                            Worker worker = new Worker();
                            worker.setName(name);
                            worker.setCoordinates(new Coordinates(Integer.parseInt(xStr), Float.parseFloat(yStr)));
                            if (!salaryStr.isEmpty() && !salaryStr.equals("0")) {
                                worker.setSalary(Float.parseFloat(salaryStr));
                            }
                            if (!positionStr.isEmpty()) {
                                try {
                                    worker.setPosition(Position.valueOf(positionStr.toUpperCase()));
                                } catch (Exception e) {}
                            }
                            worker.setStatus(Status.valueOf(statusStr.toUpperCase()));
                            worker.setPerson(new Person(
                                    birthdayStr.isEmpty() ? null : LocalDate.parse(birthdayStr),
                                    Float.parseFloat(heightStr),
                                    weightStr.isEmpty() ? null : Integer.parseInt(weightStr)
                            ));

                            // Добавляем в коллекцию
                            collectionManager.put(key, worker);
                            System.out.println("Работник добавлен с ключом " + key);
                        }

                        //  UPDATE_ID
                        else if (cmd.equals("update_id")) {
                            Integer targetId;
                            try {
                                targetId = Integer.parseInt(arg);
                            } catch (NumberFormatException e) {
                                System.out.println("Ошибка: id должен быть числом!");
                                continue;
                            }

                            // Ищем существующего работника
                            Long foundKey = null;
                            Worker oldWorker = null;
                            for (var entry : collectionManager.getCollection().entrySet()) {
                                if (entry.getValue().getId().equals(targetId)) {
                                    foundKey = entry.getKey();
                                    oldWorker = entry.getValue();
                                    break;
                                }
                            }

                            if (oldWorker == null) {
                                System.out.println("Работник с id " + targetId + " не найден");
                                // Пропускаем поля (9 строк)
                                for (int i = 0; i < 9; i++) {
                                    if (fileScanner.hasNextLine()) fileScanner.nextLine();
                                }
                                continue;
                            }

                            // Читаем поля из файла
                            String name = fileScanner.nextLine().trim();
                            String xStr = fileScanner.nextLine().trim();
                            String yStr = fileScanner.nextLine().trim();
                            String salaryStr = fileScanner.nextLine().trim();
                            String positionStr = fileScanner.nextLine().trim();
                            String statusStr = fileScanner.nextLine().trim();
                            String heightStr = fileScanner.nextLine().trim();
                            String weightStr = fileScanner.nextLine().trim();
                            String birthdayStr = fileScanner.nextLine().trim();

                            // Создаём Worker
                            Worker worker = new Worker();
                            worker.setName(name);
                            worker.setCoordinates(new Coordinates(Integer.parseInt(xStr), Float.parseFloat(yStr)));
                            if (!salaryStr.isEmpty() && !salaryStr.equals("0")) {
                                worker.setSalary(Float.parseFloat(salaryStr));
                            }
                            if (!positionStr.isEmpty()) {
                                try {
                                    worker.setPosition(Position.valueOf(positionStr.toUpperCase()));
                                } catch (Exception e) {}
                            }
                            worker.setStatus(Status.valueOf(statusStr.toUpperCase()));
                            worker.setPerson(new Person(
                                    birthdayStr.isEmpty() ? null : LocalDate.parse(birthdayStr),
                                    Float.parseFloat(heightStr),
                                    weightStr.isEmpty() ? null : Integer.parseInt(weightStr)
                            ));

                            // Сохраняем старые id и дату
                            worker.setId(oldWorker.getId());
                            worker.setCreationDate(oldWorker.getCreationDate());
                            collectionManager.put(foundKey, worker);
                            System.out.println("Работник с id " + targetId + " обновлён");
                        }

                        // ОСТАЛЬНЫЕ КОМАНДЫ
                        else {
                            Command command = commands.get(cmd);
                            if (command != null) {
                                command.execute(arg);
                            } else {
                                System.out.println("Неизвестная команда: " + cmd);
                            }
                        }
                    }

                    System.setOut(oldOut);
                    String result = baos.toString();
                    System.out.print(result);

                    System.out.println("\nСкрипт выполнен. Обработано строк: " + lineNumber);

                } catch (Exception e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            }
        });
    }  // конец метода initCommands

    /**
     * Загружает коллекцию из файла при запуске программы.
     * Что делает:
     * 1. Проверяет, существует ли файл с данными и можно ли его читать
     * 2. Читает JSON из файла и превращает обратно в коллекцию работников.
     *
     * Если файла нет или проблемный - начинаем с пустой коллекцией.
     * Файл потом создастся сам при первой команде save.
     */
    public void loadFromFile(CollectionManager collectionManager) {
        logger.info("Загрузка коллекции из файла: {}", fileName);
        File file = new File(fileName);

        // проверяем существование файла
        if (!file.exists()) {
            logger.warn("Файл {} не найден. Будет создан при сохранении.", fileName);
            return;
        }

        // проверяем, разрешила ли операционная система чтение этого файла
        if (!file.canRead()) {
            logger.error("Нет прав на чтение файла {}", fileName);
            return;
        }

        // открываем файл для чтения
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            // создаем специальный маркер типа, чтобы GSON знал, во что превращать JSON
            Type collectionType = new TypeToken<TreeMap<Long, Worker>>(){}.getType();

            // читаем данные и преобразуем их в Java-объекты
            TreeMap<Long, Worker> loaded = gson.fromJson(reader, collectionType);

            // если файл был пуст или загрузился null
            if (loaded == null) {
                logger.warn("Файл {} пуст", fileName);
                return;
            }

            // загружаем данные в collection.CollectionManager
            for (var entry : loaded.entrySet()) {
                collectionManager.put(entry.getKey(), entry.getValue());
            }

            //logger.info("Загружено {} работников из {}", collectionManager.size(), fileName);

        } catch (IOException e) {
            logger.error("Ошибка чтения файла: {}", e.getMessage());
        } catch (JsonSyntaxException e) {
            logger.error("Ошибка формата JSON в файле: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Непредвиденная ошибка: {}", e.getMessage());
        }
    }

    /**
     * Возвращает команду по её имени
     *
     * @param name имя команды
     * @return объект команды или null, если команда не найдена
     */
    public Command getCommand(String name) {
        return commands.get(name);  // .get() быстро находит по ключу
    }

}  // конец класса CommandExecutor
