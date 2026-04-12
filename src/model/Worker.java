package model;
import java.time.ZonedDateTime;
import java.io.Serializable;

/**
 * Класс работника (главный класс коллекции)
 * Хранит всю информацию о сотруднике: личные данные, должность, статус и т.д.
 * Реализует Comparable для сортировки по умолчанию (по id).
 */
public class Worker implements Comparable<Worker>, Serializable{
    private Integer id;              // генерируется автоматически, >0, уникальный
    private String name;              // не null, не пустая
    private Coordinates coordinates;  // не null
    private String creationDate;      // генерируется, не null
    private Float salary;             // может быть null, >0
    private Position position;        // может быть null
    private Status status;            // не null
    private Person person;            // не null

    // Статическое поле для автоматической генерации id
    private static int nextId = 1;

    /**
     * Конструктор по умолчанию
     * Автоматически генерирует id и дату создания
     */
    public Worker() {
        this.id = nextId++;
        this.creationDate = ZonedDateTime.now().toString();
    }


    // Геттеры (методы для получения значений)
    public Integer getId() { return id; }
    public String getName() { return name; }
    public Coordinates getCoordinates() { return coordinates; }
    public ZonedDateTime getCreationDateAsZoned() {return ZonedDateTime.parse(creationDate); }
    public String getCreationDate() {
        return creationDate;
    }
    public Float getSalary() { return salary; }
    public Position getPosition() { return position; }
    public Status getStatus() { return status; }
    public Person getPerson() { return person; }

    // Сеттеры (методы для изменения значений)
    public void setId(Integer id) {
        this.id = id;
    }
    public void setName(String name) { this.name = name; }
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }
    public void setSalary(Float salary) { this.salary = salary; }
    public void setPosition(Position position) { this.position = position; }
    public void setStatus(Status status) { this.status = status; }
    public void setPerson(Person person) { this.person = person; }

    // Сравнение для сортировки (по id)
    @Override
    public int compareTo(Worker other) {
        return this.id.compareTo(other.id);
    }

    // Для красивого вывода в консоль
    @Override
    public String toString() {
        return "Worker{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", salary="  + (salary == null ? "не указана" : salary) +
                ", position=" + position +
                ", status=" + status +
                ", person=" + person +
                '}';
    }
}
