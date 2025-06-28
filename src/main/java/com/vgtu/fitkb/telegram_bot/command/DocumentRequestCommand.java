package com.vgtu.fitkb.telegram_bot.command;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.service.PollService;
import com.vgtu.fitkb.telegram_bot.service.SubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class DocumentRequestCommand {

    private final PollService pollService;

    //private final SubmitService submitService;
    @Autowired
    public DocumentRequestCommand(PollService pollService) {
        this.pollService = pollService;
        //this.submitService = submitService;
    }

    public void execute(VGUTelegramBot bot, long chatId) {
        try {
            // 1. Вызываем PullService для получения данных
            System.out.println("Вызываем метод старт");
            startPoll(bot,chatId);
            System.out.println("Старст отработал");
            System.out.println("Вызываем метод submitData");
            System.out.println("submitData отработал");
            // 2. Если PullService отработал успешно, вызываем SubmitService
            //submitService.submitData(data);

            // 3. Отправляем сообщение об успехе
            sendMessage(bot, chatId, "Заявка успешно отправлена!");

        } catch (Exception e) {
            // 4. Обрабатываем ошибку и отправляем сообщение об неудаче
            sendMessage(bot, chatId, "Произошла ошибка при отправке заявки: " + e.getMessage());
            e.printStackTrace(); // Логируем ошибку
        }
    }


    public void startPoll(VGUTelegramBot bot, long chatId) {
        pollService.startPoll(bot, chatId);
    }

    public void processPollAnswer(VGUTelegramBot bot, long chatId, String answer) {
        pollService.processAnswer(bot, chatId, answer);
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