package com.vgtu.fitkb.telegram_bot.command;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.service.CathedraService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import com.vgtu.fitkb.telegram_bot.model.Cathedra;
@Component
public class CathedraCommand {

    private CathedraService cathedraService = new CathedraService();

    public void execute(VGUTelegramBot bot, long chatId) {
        List<Cathedra> cathedras = cathedraService.getAllCathedras(); // Получаем список кафедр
        StringBuilder messageText = new StringBuilder("Список кафедр ФИТКБ:\n");
        for (Cathedra cathedra : cathedras) {
            messageText.append("- ").append(cathedra.getName()).append("\n"); // Добавляем название кафедры в сообщение
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText.toString()); // Устанавливаем текст сообщения

        try {
            bot.execute(message); // Отправляем сообщение
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Обрабатываем ошибки при отправке сообщения
        }
    }
}