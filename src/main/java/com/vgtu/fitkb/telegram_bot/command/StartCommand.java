package com.vgtu.fitkb.telegram_bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;

@Component
public class StartCommand {
    public void execute(VGUTelegramBot bot, long chatId) {
        String answer = "Привет! Я бот ВГТУ, который поможет тебе узнать больше о факультете ФИТКБ. " +
                "Используй /help для просмотра доступных команд.";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}