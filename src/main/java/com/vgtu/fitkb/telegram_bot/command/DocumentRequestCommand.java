package com.vgtu.fitkb.telegram_bot.command;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.service.PollService;
import com.vgtu.fitkb.telegram_bot.service.SubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.service.PollService;
import com.vgtu.fitkb.telegram_bot.service.SubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class DocumentRequestCommand {

    private final PollService pollService;
    private final SubmitService submitService;

    @Autowired
    public DocumentRequestCommand(PollService pollService,
                                  SubmitService submitService) {
        this.pollService = pollService;
        this.submitService = submitService;
    }

    /**
     * Запускает процесс подачи документов
     */
    public void startPoll(VGUTelegramBot bot,long chatId) {
        // 1. Запускаем опрос
        pollService.startPoll(bot, chatId);
    }

    /**
     * Обрабатывает ответы пользователя во время опроса
     */
    public void processPollAnswer(VGUTelegramBot bot,long chatId, String answer) {
        pollService.processAnswer(bot, chatId, answer);
    }

    /**
     * Обрабатывает загруженные документы
     */
    public void processDocument(VGUTelegramBot bot,Update update) {
        long chatId = update.getMessage().getChatId();
        Document doc = update.getMessage().getDocument();

        // Сохраняем файл во временное хранилище
        submitService.processFile(bot,chatId, doc.getFileId(), doc.getFileName());

        // Отправляем подтверждение
        sendMessage(bot,chatId, "Файл " + doc.getFileName() + " получен. Отправьте следующий или нажмите 'Готово'.");
    }

    /**
     * Завершает процесс подачи документов
     */
    public void completeSubmission(VGUTelegramBot bot,long chatId) {
        submitService.completeSubmission(bot, chatId);
    }

    /**
     * Отменяет процесс подачи документов
     */
    public void cancelSubmission(VGUTelegramBot bot,long chatId) {
        submitService.cancelSubmission(bot,chatId);
        sendMessage(bot,chatId, "Процесс подачи документов отменён.");
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