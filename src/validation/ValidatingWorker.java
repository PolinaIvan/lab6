package validation;

import model.Worker;
import model.Coordinates;
import model.Person;
import model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Декоратор для валидации работников.
 * Оборачивает любой способ загрузки и добавляет проверку.
 */
public class ValidatingWorker implements WorkerLoader {

    // логгер для записи событий валидации
    private static final Logger logger = LoggerFactory.getLogger(ValidatingWorker.class);

    // поставщик данных (оборачиваемый объект)
    // Supplier дает возможность инкапсулировать логику создания объекта и
    // запустить её ровно в тот момент когда необходимо
    private final Supplier<TreeMap<Long, Worker>> loader;

    private int skippedCount = 0; // счётчик пропущенных работников

    public ValidatingWorker(Supplier<TreeMap<Long, Worker>> loader) {
        this.loader = loader;
    }

    /**
     * Основной метод интерфейса WorkerLoader.
     * Загружает данные через оборачиваемый метод, валидирует каждый объект,
     * возвращает только корректные записи.
     *
     * @return TreeMap только с валидными работниками
     */
    @Override
    public TreeMap<Long, Worker> load() {
        logger.info("Начало загрузки с валидацией");

        // вызываем оборачиваемый метод (лямбду, внутри которой loadFromFile)
        TreeMap<Long, Worker> loaded = loader.get();

        if (loaded == null || loaded.isEmpty()) {
            logger.warn("Данные не загружены или файл пуст");
            return new TreeMap<>();
        }

        logger.info("Загружено {} работников, начинаем валидацию", loaded.size());

        // валидация
        TreeMap<Long, Worker> valid = new TreeMap<>();
        skippedCount = 0;

        for (var entry : loaded.entrySet()) {
            Long key = entry.getKey();
            Worker worker = entry.getValue();

            if (isValid(worker)) {
                valid.put(key, worker);
            } else {
                skippedCount++;
                logger.warn("Пропущен работник с ключом {} (некорректные данные)", key);
            }
        }

        // вывод результата
        if (skippedCount > 0) {
            logger.info("Загрузка завершена: {} работников загружено, {} пропущено",
                    valid.size(), skippedCount);
        } else {
            logger.info("Загрузка завершена: {} работников загружено", valid.size());
        }

        return valid;
    }

    /**
     * Проверяет корректность всех полей работника.
     *
     * @param worker проверяемый работник
     * @return true если все поля валидны, false если есть ошибка
     */
    private boolean isValid(Worker worker) {
        if (worker == null) {
            logger.debug("Worker is null");
            return false;
        }

        // проверка имени
        if (worker.getName() == null || worker.getName().trim().isEmpty()) {
            logger.debug("Worker {}: имя null или пустое", worker.getId());
            return false;
        }

        // проверка координат
        if (worker.getCoordinates() == null) {
            logger.debug("Worker {}: координаты null", worker.getId());
            return false;
        }
        Coordinates coords = worker.getCoordinates();

        if (coords.getX() == null) {
            logger.debug("Worker {}: координата X null", worker.getId());
            return false;
        }
        if (coords.getX() < 1 || coords.getX() > 256) {
            logger.debug("Worker {}: координата X вне диапазона (1-256): {}", worker.getId(), coords.getX());
            return false;
        }

        if (coords.getY() == null) {
            logger.debug("Worker {}: координата Y null", worker.getId());
            return false;
        }

        // проверка статуса
        if (worker.getStatus() == null) {
            logger.debug("Worker {}: статус null", worker.getId());
            return false;
        }

        // проверка person
        if (worker.getPerson() == null) {
            logger.debug("Worker {}: Person null", worker.getId());
            return false;
        }

        if (worker.getPerson().getHeight() <= 0) {
            logger.debug("Worker {}: рост <= 0: {}", worker.getId(), worker.getPerson().getHeight());
            return false;
        }

        if (worker.getPerson().getWeight() != null && worker.getPerson().getWeight() <= 0) {
            logger.debug("Worker {}: вес <= 0: {}", worker.getId(), worker.getPerson().getWeight());
            return false;
        }

        // проверка зарплаты
        if (worker.getSalary() != null && worker.getSalary() <= 0) {
            logger.debug("Worker {}: зарплата <= 0: {}", worker.getId(), worker.getSalary());
            return false;
        }
        return true; // если все проверки пройдены
    }
}
