package com.vgtu.fitkb.telegram_bot.service;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.model.*;
import com.vgtu.fitkb.telegram_bot.model.User;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final String SPREADSHEET_ID = "1x7Ii467_rx-CZtgbr17L4RXqvXpWpx7-ZqSmqhyTywQ"; // Из URL: https://docs.google.com/spreadsheets/d/{ID}/edit
    private static final String RANGE = "Лист1!A1:G1"; // Диапазон для записи
    private static final String SHEET_NAME = "Лист1";
    private static final int ID_COLUMN_INDEX = 0;

    public Sheets getSheetsService() throws Exception {
        // Получаем ресурс из classpath
        InputStream in = getClass().getClassLoader().getResourceAsStream("vgtu-fitkb-df695196c0a3.json");

        if (in == null) {
            throw new FileNotFoundException("Credential file 'vgtu-fitkb-df695196c0a3.json' not found in classpath");
        }

        GoogleCredential credential = GoogleCredential.fromStream(in)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Your Application Name")
                .build();
    }

    public void addPersonToSheet(User user) throws Exception {
        Sheets sheetsService = getSheetsService();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String birthdayStr = user.getBirthday().format(formatter);

        // Подготовка данных
        List<Object> rowData = List.of(
                user.getId(),
                user.getFirstName(),
                user.getSecondName(),
                user.getLastName(),
                birthdayStr,
                user.getPoints(),
                user.getAvgScores()
        );

        // Запись в таблицу
        sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, RANGE,
                        new ValueRange().setValues(List.of(rowData)))
                .setValueInputOption("RAW")
                .execute();
    }

    public boolean deleteRowByUserId(String userId) throws Exception {
        Sheets sheetsService = getSheetsService();

        // 1. Находим номер строки по ID
        int rowNumber = findRowNumberByUserId(sheetsService, userId);
        if (rowNumber == -1) {
            return false; // ID не найден
        }

        // 2. Удаляем строку
        deleteRow(sheetsService, rowNumber);
        return true;
    }

    /**
     * Поиск номера строки по ID пользователя
     */
    private int findRowNumberByUserId(Sheets sheetsService, String userId) throws IOException {
        // Диапазон поиска (все данные колонки с ID)
        String range = SHEET_NAME + "!" + (char) ('A' + ID_COLUMN_INDEX) + "2:" + (char) ('A' + ID_COLUMN_INDEX);

        List<List<Object>> values = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute()
                .getValues();

        if (values == null || values.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < values.size(); i++) {
            String currentId = values.get(i).get(0).toString();
            if (userId.equals(currentId)) {
                return i + 2; // +2 потому что данные начинаются со 2-й строки (A2)
            }
        }
        return -1;
    }

    /**
     * Удаление строки по номеру
     */
    private void deleteRow(Sheets sheetsService, int rowNumber) throws IOException {
        // Запрос на удаление строки
        Request request = new Request()
                .setDeleteDimension(new DeleteDimensionRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(0) // ID листа (0 для первого листа)
                                .setDimension("ROWS")
                                .setStartIndex(rowNumber - 1) // Нумерация с 0
                                .setEndIndex(rowNumber)));

        // Выполнение batch-запроса
        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(List.of(request));

        sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, batchRequest).execute();
    }
}