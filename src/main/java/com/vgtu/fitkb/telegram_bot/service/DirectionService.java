package com.vgtu.fitkb.telegram_bot.service;

import com.vgtu.fitkb.telegram_bot.model.Cathedra;
import com.vgtu.fitkb.telegram_bot.model.Direction;
import org.springframework.stereotype.Service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DirectionService {

    private static final List<Direction> directions = new ArrayList<>();

    static {
        // Добавляем направления (заглушка, нужно заменить реальными данными)
        Direction direction1 = new Direction();
        direction1.setName("09.03.01 Информатика и вычислительная техника");
        direction1.setDescription("Описание направления 09.03.01..."); // Добавьте описание
        directions.add(direction1);


        // Добавьте другие направления...
    }

    public List<Direction> getAllDirections() {
        return directions;
    }

    public Direction getDirectionByName(String name) {
        for (Direction direction : directions) {
            if (direction.getName().equalsIgnoreCase(name)) { // Игнорируем регистр
                return direction;
            }
        }
        return null; // Если направление не найдено
    }
}