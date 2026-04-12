package collection;

import model.Worker;
import java.util.TreeMap;

/**
 * Класс для управления коллекцией работников.
 * Содержит TreeMap для хранения пар ключ-значение, где
 * ключ - уникальный идентификатор Long,
 * значение - объект Worker.
 */
public class CollectionManager {

    // Коллекция для хранения работников
    private TreeMap<Long, Worker> collection = new TreeMap<>();

    /**
     * Добавляет работника в коллекцию по указанному ключу.
     *
     * @param key ключ для доступа к работнику
     * @param worker объект работника
     */
    public void put(Long key, Worker worker) {
        collection.put(key, worker);
    }

    /**
     * Возвращает работника по ключу.
     *
     * @param key ключ работника
     * @return объект Worker или null, если ключ не найден
     */
    public Worker get(Long key) {
        return collection.get(key);
    }

    /**
     * Удаляет работника по ключу.
     *
     * @param key ключ работника для удаления
     */
    public void remove(Long key) {
        collection.remove(key);
    }

    /**
     * Проверяет, существует ли ключ в коллекции.
     *
     * @param key проверяемый ключ
     * @return true если ключ существует, false в противном случае
     */
    public boolean containsKey(Long key) {
        return collection.containsKey(key);
    }

    /**
     * Возвращает всю коллекцию
     *
     * @return TreeMap с работниками
     */
    public TreeMap<Long, Worker> getCollection() {
        return collection;
    }

    /**
     * Возвращает количество элементов в коллекции
     *
     * @return размер коллекции
     */
    public int size() {
        return collection.size();
    }

    /**
     * Проверяет, пустая ли коллекция
     *
     * @return true если коллекция пуста, false в ней что-то есть
     */
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    /**
     * Очищает коллекцию (удаляет все элементы).
     */
    public void clear() {
        collection.clear();
    }
}