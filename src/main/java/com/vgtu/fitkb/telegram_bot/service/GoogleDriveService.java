package com.vgtu.fitkb.telegram_bot.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot; // Import TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile; // Import GetFile
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDriveService {

    private final Drive driveService;
    private final String rootFolderId;
     // Add TelegramBot instance

    public GoogleDriveService(
            @Value("${google.drive.credentials-file}") Resource credentialsFile,
            @Value("${google.drive.folder-id}") String rootFolderId
    ) throws IOException, GeneralSecurityException {
        this.rootFolderId = rootFolderId;
         // Initialize TelegramBot

        // Загрузка учетных данных из JSON-файла
        InputStream in = credentialsFile.getInputStream();
        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(Collections.singleton(DriveScopes.DRIVE));

        // Инициализация Drive API
        this.driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("VGTU-FITKB-Bot")
                .build();
    }

    /**
     * Создает папку в Google Drive
     * @param folderName Название папки
     * @return ID созданной папки
     */
    public String createFolder(String folderName) throws IOException {
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(rootFolderId));

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }

    /**
     * Загружает файл в Google Drive
     * @param fileContent Содержимое файла в виде byte[]
     * @param fileName Имя файла
     * @param folderId ID папки назначения
     * @return ID загруженного файла
     */
    public String uploadFile(byte[] fileContent, String fileName, String folderId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderId));

        InputStreamContent mediaContent = new InputStreamContent(
                null,
                new ByteArrayInputStream(fileContent)
        );

        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        return file.getId();
    }

    public String uploadTelegramFile(VGUTelegramBot telegramBot,String fileId, String fileName, String folderId) throws IOException {
        // 1. Получаем файл из Telegram API
        byte[] fileContent = downloadFileFromTelegram(telegramBot,fileId);

        // 2. Загружаем в Google Drive
        return uploadFile(fileContent, fileName, folderId);
    }

    private byte[] downloadFileFromTelegram(VGUTelegramBot telegramBot, String fileId) throws IOException {
        // Реализация загрузки файла из Telegram
        // Вам нужно использовать Telegram Bot API для получения файла
        // 1. Get file path from Telegram
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);

        String filePath = null;
        try {
            filePath = telegramBot.execute(getFile).getFilePath();
        } catch (TelegramApiException e) {
            throw new IOException("Error getting file path from Telegram", e);
        }

        // 2. Download file from Telegram
        URL url = new URL("https://api.telegram.org/file/bot" + telegramBot.getBotToken() + "/" + filePath);
        try (InputStream is = url.openStream()) {
            return is.readAllBytes();
        }
    }
    /**
     * Загружает документ из Telegram в Google Drive
     * @param document Документ из Telegram
     * @param folderId ID папки назначения
     * @return ID загруженного файла
     */
    public String uploadTelegramDocument(VGUTelegramBot telegramBot,Document document, String folderId) throws IOException {
        byte[] fileContent = getFileContentFromTelegram(telegramBot,document);
        return uploadFile(fileContent, document.getFileName(), folderId);
    }

    /**
     * Получает список файлов в папке
     * @param folderId ID папки
     * @return Список файлов
     */
    public List<File> listFiles(String folderId) throws IOException {
        FileList result = driveService.files().list()
                .setQ("'" + folderId + "' in parents")
                .setFields("files(id, name, mimeType, size, modifiedTime)")
                .execute();

        return result.getFiles();
    }

    private byte[] getFileContentFromTelegram(VGUTelegramBot telegramBot,Document document) throws IOException {
        return downloadFileFromTelegram(telegramBot,document.getFileId());
    }
}