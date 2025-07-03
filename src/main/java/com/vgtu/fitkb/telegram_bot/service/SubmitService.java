package com.vgtu.fitkb.telegram_bot.service;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubmitService {
    private final UserService userService;
    private final GoogleDriveService driveService;
    private final Map<Long, List<String>> userFiles = new ConcurrentHashMap<>();

    @Autowired
    public SubmitService(GoogleDriveService driveService,UserService userService) {
        this.driveService = driveService;
        this.userService = userService;
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

    public boolean completeSubmission(VGUTelegramBot bot, Long chatId) throws Exception {
        List<String> files = userFiles.get(chatId);
        if (files == null || files.isEmpty()) {
            return false; // Не удаляем usersUploadingFiles здесь!
        }

        try {
            sendMessage(bot,chatId,"Пожалуйста подождите, документы отправляются.");
            User user = userService.findByChatId(chatId);
            String folderId = driveService.createFolder(user.getSecondName() + " " + user.getFirstName()+ " " + user.getLastName() + " "+ user.getBirthday());
            for (String file : files) {
                String[] parts = file.split(":");
                driveService.uploadTelegramFile(bot, parts[1], parts[0], folderId);
            }
            sendMessage(bot, chatId, "✅ Документы успешно загружены!");
            return true;
        } catch (Exception e) {
            throw new Exception();
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