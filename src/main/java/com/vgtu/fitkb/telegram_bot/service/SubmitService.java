package com.vgtu.fitkb.telegram_bot.service;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubmitService {
    private final GoogleDriveService driveService;
    private final Map<Long, List<String>> userFiles = new ConcurrentHashMap<>();

    @Autowired
    public SubmitService(GoogleDriveService driveService) {
        this.driveService = driveService;
    }

    public void requestDocuments(VGUTelegramBot bot,Long chatId, User userData) {
        // Сохраняем данные пользователя временно
        userFiles.put(chatId, new ArrayList<>());

        // Отправляем инструкцию
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("""
            📎 Пожалуйста, загрузите необходимые документы:
            - Паспорт (первая страница и прописка)
            - Аттестат/диплом
            - Фото 3x4
            - Другие подтверждающие документы
            
            Отправляйте файлы по одному. Когда закончите, нажмите "Готово".
            """);

        // Создаем клавиатуру с кнопкой "Готово"
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Готово");
        row.add("Отмена");
        rows.add(row);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void processFile(VGUTelegramBot bot,Long chatId, String fileId, String fileName) {
        List<String> files = userFiles.getOrDefault(chatId, new ArrayList<>());
        files.add(fileName + ":" + fileId); // Сохраняем имя и ID файла
        userFiles.put(chatId, files);

        sendMessage(bot,chatId, "Файл " + fileName + " получен. Отправьте следующий или нажмите 'Готово'.");
    }

    public void completeSubmission(VGUTelegramBot bot,Long chatId) {
        List<String> files = userFiles.get(chatId);
        if (files == null || files.isEmpty()) {
            sendMessage(bot,chatId, "Вы не отправили ни одного файла. Попробуйте снова.");
            return;
        }

        try {
            // 1. Создаем папку для пользователя в Google Drive
            String folderId = driveService.createFolder(chatId.toString());

            // 2. Загружаем все файлы
            for (String file : files) {
                String[] parts = file.split(":");
                String fileName = parts[0];
                String fileId = parts[1];
                driveService.uploadTelegramFile(bot,fileId, fileName, folderId);
            }

            // 3. Отправляем подтверждение
            sendMessage(bot,chatId, "✅ Все документы успешно загружены в облако!");

        } catch (Exception e) {
            sendMessage(bot,chatId, "❌ Ошибка при загрузке документов: " + e.getMessage());
            e.printStackTrace();
        } finally {
            userFiles.remove(chatId); // Очищаем временные данные
        }
    }

    public void cancelSubmission(VGUTelegramBot bot,Long chatId) {
        userFiles.remove(chatId);
        sendMessage(bot,chatId, "Загрузка документов отменена.");
    }

    private void sendMessage(VGUTelegramBot bot, Long chatId, String text) {
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