package com.vgtu.fitkb.telegram_bot.command;


import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.model.Direction;
import com.vgtu.fitkb.telegram_bot.service.DirectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class DirectionCommand {

    private final DirectionService directionService;

    @Autowired
    public DirectionCommand(DirectionService directionService) {
        this.directionService = directionService;
    }

    public void showDirectionList(TelegramLongPollingBot bot, long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите направление:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        List<Direction> directions = directionService.getAllDirections();

        // Добавляем кнопки с названиями направлений
        for (Direction direction : directions) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(direction.getName()));
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
    public Direction getDirectionBySecondName(String secondName){
        return directionService.getDirectionBySecondName(secondName);
    }
    // Нет метода sendDirectionInfo
}