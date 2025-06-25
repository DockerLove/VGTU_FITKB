package com.vgtu.fitkb.telegram_bot.command;


import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.model.Direction;
import com.vgtu.fitkb.telegram_bot.service.DirectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class DirectionCommand {


    private DirectionService directionService;

    @Autowired
    public DirectionCommand(DirectionService directionService) {
        this.directionService = directionService;
    }

    public void execute(VGUTelegramBot bot, long chatId) {
        List<Direction> directions = directionService.getAllDirections();
        StringBuilder messageText = new StringBuilder("Список направлений ФИТКБ:\n");
        for (Direction direction : directions) {
            messageText.append("- ").append(direction.getName()).append(" (Кафедра: ").append(direction.getCathedra()).append(")\n"); // Указываем название и кафедру
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText.toString());

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}