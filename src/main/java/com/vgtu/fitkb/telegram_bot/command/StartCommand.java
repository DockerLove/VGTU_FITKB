package com.vgtu.fitkb.telegram_bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;

import java.io.File;

@Component
public class StartCommand {
    public void execute(VGUTelegramBot bot, long chatId) {
        // Путь к картинке (замени на свой путь)
        String photoPath = "src/main/resources/vgtu_fitkb.jpg"; // Например, картинка в папке resources

        // Создаем объект SendPhoto
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));

        // Указываем картинку
        InputFile inputFile = new InputFile(new File(photoPath));
        sendPhoto.setPhoto(inputFile);

        // Текст сообщения
        String text = "Привет! Я бот ВГТУ, который поможет тебе узнать больше о факультете ФИТКБ.\n\n " +
                "Факультет был организован как самостоятельная структурная единица университета в 2013 году," +
                " объединив в себе подготовку специалистов в области IT-технологий. Информационные технологии - " +
                "широкий класс дисциплин и областей деятельности, относящихся к формированию, управлению и обработке " +
                "массивов данных с акцентом на применение средств вычислительной техники. На факультете осуществляется " +
                "подготовка бакалавров, магистров, специалистов и аспирантов." +
                "\n\nИспользуй /help для просмотра доступных команд.";

        // Отправляем фото с подписью
        sendPhoto.setCaption(text); // Добавляем подпись к фото

        try {
            bot.execute(sendPhoto); // Отправляем фото
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}