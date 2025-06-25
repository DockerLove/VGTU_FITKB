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

    public void execute(VGUTelegramBot bot, long chatId, String cathedraSecondName) {
        if (cathedraSecondName == null || cathedraSecondName.isEmpty()) {
            // Если параметр не указан, выводим список всех кафедр
            List<Cathedra> cathedras = cathedraService.getAllCathedras();
            StringBuilder messageText = new StringBuilder("Список кафедр ФИТКБ:\n");
            for (Cathedra cathedra : cathedras) {
                messageText.append("- ").append(cathedra.getName()).append("\n");
            }
            sendMessage(bot, chatId, messageText.toString());
        } else {
            // Если параметр указан, ищем кафедру по названию
            Cathedra cathedra = cathedraService.getCathedraBySecondName(cathedraSecondName);
            if (cathedra != null) {
                String messageText = "Информация о кафедре " + cathedra.getName() + ":\n" + cathedra.getDescription();
                sendMessage(bot, chatId, messageText);
            } else {
                sendMessage(bot, chatId, "Кафедра с названием '" + cathedraSecondName + "' не найдена.");
            }
        }
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