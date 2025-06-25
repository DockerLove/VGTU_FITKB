package com.vgtu.fitkb.telegram_bot.service;
import com.vgtu.fitkb.telegram_bot.model.Cathedra;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CathedraService {

    public List<Cathedra> getAllCathedras() {
        List<Cathedra> cathedras = new ArrayList<>();

        // Добавляем кафедры (заглушка, нужно заменить реальными данными)
        Cathedra cathedra1 = new Cathedra();
        cathedra1.setName("Кафедра Информационных и интеллектуальных технологий (ИИТ)");
        cathedra1.setDescription("Описание кафедры ИИТ..."); // Добавь описание
        cathedras.add(cathedra1);

        Cathedra cathedra2 = new Cathedra();
        cathedra2.setName("Кафедра Программного обеспечения вычислительной техники и автоматизированных систем (ПОВТ)");
        cathedra2.setDescription("Описание кафедры ПОВТ..."); // Добавь описание
        cathedras.add(cathedra2);

        // Добавь другие кафедры...

        return cathedras;
    }

    // Можно добавить методы для получения информации о конкретной кафедре,
    // поиска кафедры по названию и т.д.
}