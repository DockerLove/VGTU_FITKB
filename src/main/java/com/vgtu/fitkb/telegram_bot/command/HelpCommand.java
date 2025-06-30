package com.vgtu.fitkb.telegram_bot.command;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class HelpCommand {

    public void execute(VGUTelegramBot bot, long chatId) {
        String helpMessage = "Список доступных команд:\n" +
                "/star - Начать общение с ботом\n" +
                "/help - Показать это сообщение\n" +
                "/cathedra - Получить список и информацию по каждой кафедре ФИТКБ\n" +
                "/direction - Получить список и информацию по каждому направлению ФИТКБ\n" +
                "/dormitory - Получить информацию об общежитии\n" +
                "/docs - Получить файл с информацей о подаче заявления в общежитие\n" +
                "/submit - Подать заявление на общежитие\n" +
                "/rating - Узнать на каком вы месте в рейтинге на зачисление в общежитие"; // Добавим описание команды /direction

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(helpMessage);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}