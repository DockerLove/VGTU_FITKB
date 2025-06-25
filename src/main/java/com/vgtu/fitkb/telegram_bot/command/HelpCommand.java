package com.vgtu.fitkb.telegram_bot.command;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class HelpCommand {

    public void execute(VGUTelegramBot bot, long chatId) {
        String helpMessage = "Список доступных команд:\n" +
                "/start - Начать общение с ботом\n" +
                "/help - Показать это сообщение\n" +
                "/cathedra - Получить список кафедр ФИТКБ\n" +
                "/cathedra X - Получить информацию по кафедре. Пример /cathеdra КИТП \n" +
                "/direction - Получить список направлений ФИТКБ\n" +
                "/direction X - Получить информацию по направлению. Пример /dirеction 09.03.02 \n" +
                "/dormitory - Получить информацию об общежитии\n" +
                "/docs - Получить файл с информацей о подаче заявления в общежитие"; // Добавим описание команды /direction

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