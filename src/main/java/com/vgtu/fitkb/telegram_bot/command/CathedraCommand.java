package com.vgtu.fitkb.telegram_bot.command;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.service.CathedraService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import com.vgtu.fitkb.telegram_bot.model.Cathedra;
@Component
public class CathedraCommand {

    private final CathedraService cathedraService;

    @Autowired
    public CathedraCommand(CathedraService cathedraService) {
        this.cathedraService = cathedraService;
    }

    public void showCathedraList(TelegramLongPollingBot bot, long chatId) {
        StringBuilder text = new StringBuilder();

        // Добавляем список кафедр с полными названиями
        List<Cathedra> cathedras = cathedraService.getAllCathedras();
        for (Cathedra cathedra : cathedras) {
            text.append("- ").append(cathedra.getName()).append("\n\n"); // Добавляем полное название
        }
        text.append("\nВыберите кафедру:"); // Добавляем "Выберите кафедру:" после списка

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text.toString());  // Устанавливаем составной текст

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Добавляем кнопки с краткими названиями кафедр
        for (Cathedra cathedra : cathedras) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(cathedra.getSecondName()));
            keyboardRows.add(row);
        }

        // Добавляем кнопку "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("Назад"));
        keyboardRows.add(backRow);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public Cathedra getCathedraBySecondName(String secondName){
        return cathedraService.getCathedraBySecondName(secondName);

    }
}