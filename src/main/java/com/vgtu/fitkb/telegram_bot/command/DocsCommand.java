package com.vgtu.fitkb.telegram_bot.command;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.nio.charset.StandardCharsets;


@Component
public class DocsCommand {

    private final String documentsPdfPath;

    public DocsCommand(@Value("${documents.pdf.path}") String documentsPdfPath) {
        this.documentsPdfPath = documentsPdfPath;
    }

    public void execute(TelegramLongPollingBot bot, long chatId) {
        // 1. Отправляем текст
        sendDocumentDescription(bot, chatId, "Список документов для подачи заявления на общежитие:");

        // 2. Отправляем PDF-файл
        sendPdfFile(bot, chatId);
    }

    private void sendDocumentDescription(TelegramLongPollingBot bot, long chatId, String description) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(description);
            bot.execute(message);
        } catch (TelegramApiException e) {
            // Обработка ошибки отправки текстового сообщения
        }
    }

    private void sendPdfFile(TelegramLongPollingBot bot, long chatId) {
        File pdfFile = new File(documentsPdfPath);

        if (!pdfFile.exists() || !pdfFile.isFile()) {
            sendErrorMessage(bot, chatId, "PDF file not found");
            return;
        }

        try {
            InputFile inputFile = new InputFile(pdfFile);
            SendDocument document = new SendDocument(String.valueOf(chatId), inputFile);
            bot.execute(document);
        } catch (TelegramApiException e) {
            sendErrorMessage(bot, chatId, "Failed to send PDF file");
        }
    }


    private void sendErrorMessage(TelegramLongPollingBot bot, long chatId, String errorMessage) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(errorMessage);
            bot.execute(message);
        } catch (TelegramApiException e) {
            // Логировать ошибку
        }
    }
}