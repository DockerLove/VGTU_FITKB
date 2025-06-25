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

    public void execute(VGUTelegramBot bot, long chatId, String directionName) {
        if (directionName == null || directionName.isEmpty()) {
            // Если параметр не указан, выводим список всех направлений
            List<Direction> directions = directionService.getAllDirections();
            StringBuilder messageText = new StringBuilder("Список направлений ФИТКБ:\n");
            for (Direction direction : directions) {
                messageText.append("- ").append(direction.getName()).append("\n");
            }
            sendMessage(bot, chatId, messageText.toString());
        } else {
            // Если параметр указан, ищем направление по названию
            Direction direction = directionService.getDirectionBySecondName(directionName);
            if (direction != null) {
                String messageText = "Информация о направлении " + direction.getName() + ":\n" + direction.getDescription(); // Добавляем информацию о кафедре
                sendMessage(bot, chatId, messageText);
            } else {
                sendMessage(bot, chatId, "Направление с названием '" + directionName + "' не найдено.");
            }
        }
    }

    private void sendMessage(VGUTelegramBot bot, long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}