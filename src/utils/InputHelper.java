package utils;

import model.*;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Класс для обработки пользовательского ввода.
 * Содержит методы для чтения различных типов данных
 * с повторным запросом при ошибках.
 */
public class InputHelper {
    private Scanner scanner;

    /**
     * Конструктор класса InputHelper.
     *
     * @param scanner сканер для чтения ввода
     */
    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    //_Основной метод для ввода работника_

    /**
     * Полный ввод работника со всеми полями.
     * Используется в insert, update, replace_if_lower.
     *
     * @return Worker объект или null при ошибке
     */
    public Worker readWorkerForComparison() {
        try {
            //создаем нового работника
            Worker worker = new Worker();  //id и creationDate генерируются автоматически

            //начинаем заполнять поля
            System.out.println("\n___ Ввод данных работника ___");

            // Имя
            System.out.print("Введите имя: ");
            String name = scanner.nextLine().trim(); //запишем то, что напечатается консоли
            while (name.isEmpty()) {
                System.out.print("Имя не может быть пустым! Введите снова: ");
                name = scanner.nextLine().trim();
            }
            //проверяем на число
            while (name.matches("-?\\d+(\\.\\d+)?")) {  // если строка состоит только из цифр
                System.out.print("Имя не может быть числом! Введите текст: ");
                name = scanner.nextLine().trim();
            }
            worker.setName(name);

            // Координаты
            System.out.println("\n___ Ввод координат ___");
            System.out.print("Введите X (целое число, максимум 256): ");
            Integer x = readIntWithBounds(1, 256);  //
            System.out.print("Введите Y (дробное число): ");
            Float y = readFloat();

            Coordinates coords = new Coordinates(x, y);
            worker.setCoordinates(coords);

            // Зарплата
            boolean isValid = false;
            while (!isValid) {
                System.out.print("Введите зарплату (Enter - пропустить): ");
                String salaryStr = scanner.nextLine().trim().replace(',', '.');  // читаем строку

                if (!salaryStr.isEmpty() && !salaryStr.equals("0")) { //если пользователь что-то ввел и не 0
                    try {
                        float salary = Float.parseFloat(salaryStr); //пробуем превратить строку в число

                        if (salary < 0) {
                            System.out.println("Зарплата не может быть отрицательной. Попробуйте ещё раз");
                            continue;
                        } else {
                            isValid = true; //выходим из цикла
                            worker.setSalary(salary);
                        }
                    } catch (NumberFormatException e) {
                        //если возникла ошибка — значит в строке были буквы или лишние символы
                        System.out.println("Введите корректное число без букв и других лишних символов");
                    }
                } else {
                    //выполняется если пользователь сразу нажал Enter или 0
                    if (salaryStr.isEmpty()){
                        worker.setSalary(0f);
                    }
                    System.out.println("Зарплата не указана");
                    isValid = true; //выходим из цикла
                }
            }

            // Должность (enum)
            while (true) {
                System.out.println("\nДоступные должности:");
                for (Position p : Position.values()) {
                    System.out.println("  - " + p);
                }
                System.out.print("Введите должность (Enter - пропустить): ");

                String posStr = scanner.nextLine().trim().toUpperCase();

                if (posStr.isEmpty()) { break; } //если нажали Enter (пустая строка) — выходим из цикла

                try {
                    //конвертируем строку в элемент enum (если текст совпал с названием должности)
                    Position position = Position.valueOf(posStr);
                    worker.setPosition(position);
                    break; //успешно установили — выходим из цикла
                } catch (IllegalArgumentException e) {
                    System.out.println("Такой должности не существует. Попробуйте еще раз");
                }
            }

            // Статус (enum, обязательный)
            System.out.println("\nДоступные статусы:");
            for (Status s : Status.values()) {
                System.out.println("  - " + s);
            }
            System.out.print("Введите статус: ");
            String statusStr = scanner.nextLine().trim().toUpperCase();
            Status status = null;
            while (status == null) {
                try {
                    status = Status.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    System.out.print("Неверный статус! Введите снова: ");
                    statusStr = scanner.nextLine().trim().toUpperCase();
                }
            }
            worker.setStatus(status);

            // Личные данные (Person)
            System.out.println("\n=== Ввод личных данных ===");

            System.out.print("Введите рост (число > 0): ");
            float height = readFloatGreaterThan(0);

            System.out.print("Введите вес (Enter - пропустить): ");
            String weightStr = scanner.nextLine().trim();
            Integer weight = null;
            if (!weightStr.isEmpty()) {
                try {
                    weight = Integer.parseInt(weightStr);
                    while (weight <= 0) {
                        System.out.print("Вес должен быть > 0! Введите снова: ");
                        weight = Integer.parseInt(scanner.nextLine());
                    }
                } catch (NumberFormatException e){
                    System.out.println("Вес должен быть числом!");
                }
            }

            LocalDate birthday = null;
            while (true){
                System.out.print("Введите дату рождения (ГГГГ-ММ-ДД, Enter - пропустить): ");
                String dateStr = scanner.nextLine().trim();
                if (!dateStr.isEmpty()) {
                    try {
                        birthday = LocalDate.parse(dateStr);
                        break;
                    } catch (Exception e) {
                        System.out.println("Неверный формат даты, попробуйте ещё раз");
                    }
                } else {
                    System.out.println("Дата рождения пропущена.");
                    break;
                }
            }

            Person person = new Person(birthday, height, weight);
            worker.setPerson(person);

            return worker; //в итоге метода возвращаем созданного работника
        } catch (Exception e) {
            //если сюда попали - случилась непредвиденная ошибка
            System.out.println("Произошла непредвиденная ошибка");
            System.out.println("Попробуйте еще раз");
            return null;
        }
    }

    //_Вспомогательные методы_

    /**
     * Читает целое число с проверкой на границы.
     *
     * @param min минимальное значение
     * @param max максимальное значение
     * @return введенное число
     */
    private Integer readIntWithBounds(int min, int max) {
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) return val; //если всё ок, возвращаем число и выходим из метода
                System.out.print("Число должно быть от " + min + " до " + max + "! Попробуйте снова: ");
            } catch (Exception e) {
                //сработает, если ввели буквы или пустую строку
                System.out.print("Ошибка! Введите целое число: ");
            }
        }
    }

    /**
     * Читает дробное число.
     *
     * @return введенное число
     */
    private Float readFloat() {
        while (true) {
            try {
                String input = scanner.nextLine().replace(',', '.');
                return Float.parseFloat(input); //если конвертация успешна — возвращаем результат
            } catch (Exception e) {
                //если в строке буквы или она пустая
                System.out.print("Ошибка! Введите число : ");
            }
        }
    }

    /**
     * Читает дробное число больше заданного.
     *
     * @param min минимальное значение
     * @return введенное число
     */
    private float readFloatGreaterThan(float min) {
        while (true) {
            try {
                float val = Float.parseFloat(scanner.nextLine().trim().replace(',', '.'));
                if (val > min) return val;
                System.out.print("Нужно число > " + min + ": ");
            } catch (Exception e) {
                System.out.print("Ошибка! Введите число: ");
            }
        }
    }
}