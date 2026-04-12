package validation;

import model.Worker;
import java.util.TreeMap;

/**
 * Интерфейс для загрузки работников.
 * Базовый компонент для паттерна Decorator.
 */
public interface WorkerLoader {
    TreeMap<Long, Worker> load();
}
