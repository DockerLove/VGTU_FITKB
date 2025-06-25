package com.vgtu.fitkb.telegram_bot.service;

import com.vgtu.fitkb.telegram_bot.model.Direction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DirectionService {

    public List<Direction> getAllDirections() {
        List<Direction> directions = new ArrayList<>();

        // Добавляем направления (заглушка, нужно заменить реальными данными)
        Direction direction1 = new Direction();
        direction1.setName("Информационные системы и технологии");
        direction1.setCathedra("Кафедра Информационных и интеллектуальных технологий (ИИТ)"); // Указываем кафедру
        directions.add(direction1);

        Direction direction2 = new Direction();
        direction2.setName("Программная инженерия");
        direction2.setCathedra("Кафедра Программного обеспечения вычислительной техники и автоматизированных систем (ПОВТ)"); // Указываем кафедру
        directions.add(direction2);

        // Добавь другие направления...

        return directions;
    }

    // Можно добавить методы для получения информации о конкретном направлении,
    // поиска направления по названию и т.д.
}