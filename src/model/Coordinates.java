package model;
import java.io.Serializable;

/**
 * Класс координат работника.
 * Содержит координаты X и Y для определения местоположения.
 */
public class Coordinates implements Serializable {
    private Integer x; // максимальное значение: 256, не может быть null
    private Float y;   // не может быть null

    public Coordinates(Integer x, Float y) {
        this.x = x;
        this.y = y;
    }

    // геттеры
    public Integer getX() { return x; }
    public Float getY() { return y; }

    // сеттеры
    public void setX(Integer x) { this.x = x; }
    public void setY(Float y) { this.y = y; }

    @Override
    public String toString() {
        return "Coordinates{x=" + x + ", y=" + y + "}";
    }
}
