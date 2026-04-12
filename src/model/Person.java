package model;
import java.time.LocalDate;
import java.io.Serializable;

/**
 * Класс с личными данными работника.
 * Содержит информацию о росте, весе и дате рождения.
 */
public class Person implements Serializable{
    private String birthday; // может быть null
    private float height;       // должно быть больше 0
    private Integer weight;     // может быть null, должно быть больше 0

    public Person(LocalDate birthday, float height, Integer weight) {
        if (birthday != null) {
            this.birthday = birthday.toString();  // превращаем дату в строку
        } else {
            this.birthday = null;
        }
        this.height = height;
        this.weight = weight;
    }

    // геттеры
    public String getBirthday() { return birthday; }
    public LocalDate getBirthdayAsLocalDate() {
        if (birthday == null) return null;
        return LocalDate.parse(birthday);  // строка переводится обратно в дату
    }
    public float getHeight() { return height; }
    public Integer getWeight() { return weight; }

    @Override
    public String toString() {
        return "Person{" + "birthday=" + birthday + ", height=" + height + ", weight=" + weight + '}';
    }
}