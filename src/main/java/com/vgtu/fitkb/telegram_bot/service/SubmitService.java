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

    public void processFile(VGUTelegramBot bot, Long chatId, String fileId, String fileName) {
        List<String> files = userFiles.getOrDefault(chatId, new ArrayList<>());

        // Проверяем, не был ли файл уже добавлен
        boolean fileAlreadyExists = files.stream()
                .anyMatch(f -> f.split(":")[1].equals(fileId));

        if (!fileAlreadyExists) {
            files.add(fileName + ":" + fileId);
            userFiles.put(chatId, files);
        }
    }

    public boolean completeSubmission(VGUTelegramBot bot, Long chatId) {
        List<String> files = userFiles.get(chatId);
        if (files == null || files.isEmpty()) {
            return false; // Не удаляем usersUploadingFiles здесь!
        }

        try {
            sendMessage(bot,chatId,"Пожалуйста подождите, документы отправляются.");
            String folderId = driveService.createFolder(chatId.toString());
            for (String file : files) {
                String[] parts = file.split(":");
                driveService.uploadTelegramFile(bot, parts[1], parts[0], folderId);
            }
            sendMessage(bot, chatId, "✅ Документы успешно загружены!");
            return true;
        } catch (Exception e) {
            sendMessage(bot, chatId, "❌ Ошибка: " + e.getMessage());
            return false;
        } finally {
            userFiles.remove(chatId);
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