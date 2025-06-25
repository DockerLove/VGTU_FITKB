package com.vgtu.fitkb.telegram_bot.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import com.vgtu.fitkb.telegram_bot.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.vgtu.fitkb.telegram_bot.command.StartCommand;
import com.vgtu.fitkb.telegram_bot.command.HelpCommand;
import com.vgtu.fitkb.telegram_bot.command.CathedraCommand;
import com.vgtu.fitkb.telegram_bot.command.DirectionCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class VGUTelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final StartCommand startCommand;
    private final HelpCommand helpCommand;
    private final CathedraCommand cathedraCommand;
    private final DirectionCommand directionCommand;

    @Autowired // Добавляем @Autowired для конструктора
    public VGUTelegramBot(BotConfig config, StartCommand startCommand, HelpCommand helpCommand, CathedraCommand cathedraCommand, DirectionCommand directionCommand) {
        this.config = config;
        this.startCommand = startCommand;
        this.helpCommand = helpCommand;
        this.cathedraCommand = cathedraCommand;
        this.directionCommand = directionCommand;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("/")) {
                String[] parts = messageText.substring(1).split(" ", 2); // Разделяем команду и параметры
                String command = parts[0];
                String parameter = (parts.length > 1) ? parts[1] : null; // Получаем параметр, если он есть
                switch (command) {
                    case "start":
                        startCommand.execute(this, chatId);
                        break;
                    case "help":
                        helpCommand.execute(this, chatId);
                        break;
                    case "cathedra":
                        cathedraCommand.execute(this, chatId, parameter);
                        break;
                    case "direction":
                        directionCommand.execute(this, chatId, parameter);
                        break;
                    default:
                        sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд..");
                }
            } else {
                sendMessage(chatId, "Я понимаю только команды. Используйте /help для просмотра доступных команд.");
            }
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}