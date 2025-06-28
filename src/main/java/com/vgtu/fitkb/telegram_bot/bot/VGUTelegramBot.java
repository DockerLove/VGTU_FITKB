package com.vgtu.fitkb.telegram_bot.bot;

import com.vgtu.fitkb.telegram_bot.command.*;
import com.vgtu.fitkb.telegram_bot.model.Cathedra;
import com.vgtu.fitkb.telegram_bot.model.Direction;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import com.vgtu.fitkb.telegram_bot.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VGUTelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final StartCommand startCommand;
    private final HelpCommand helpCommand;
    private final CathedraCommand cathedraCommand;
    private final DirectionCommand directionCommand;
    private final DocumentRequestCommand documentRequestCommand;
    private final DormitoryCommand dormitoryCommand;
    private final DocsCommand docsCommand;
    private final Map<Long, Boolean> usersInPoll = new HashMap<>();

    private static final String HELP_COMMAND = "/help";
    private static final String DOCS_COMMAND = "/docs";
    private static final String DORMITORY_COMMAND = "/dormitory";
    private static final String CATHEDRA_COMMAND = "/cathedra";
    private static final String DIRECTION_COMMAND = "/direction";
    private static final String SUBMIT_DOCUMENTS = "/submit";
    private final Map<Long, Boolean> userShowMainKeyboard = new HashMap<>();

    private boolean showMainKeyboard = true;
    // Соответствие русских названий и английских команд
    private static final Map<String, String> commandMap = new HashMap<>();

    static {
        commandMap.put("Список команд", HELP_COMMAND);
        commandMap.put("Документы", DOCS_COMMAND);
        commandMap.put("Общежитие", DORMITORY_COMMAND);
        commandMap.put("Кафедры", CATHEDRA_COMMAND);
        commandMap.put("Направления", DIRECTION_COMMAND);
        commandMap.put("Назад", "/back");
        commandMap.put("Подать документы", SUBMIT_DOCUMENTS);
    }
    @Autowired
    public VGUTelegramBot(BotConfig config, StartCommand startCommand, HelpCommand helpCommand,
                          CathedraCommand cathedraCommand, DirectionCommand directionCommand,
                          DormitoryCommand dormitoryCommand,DocsCommand docsCommand, DocumentRequestCommand documentRequestCommand) {
        this.config = config;
        this.startCommand = startCommand;
        this.helpCommand = helpCommand;
        this.cathedraCommand = cathedraCommand;
        this.directionCommand = directionCommand;
        this.dormitoryCommand = dormitoryCommand;
        this.docsCommand = docsCommand;
        this.documentRequestCommand = documentRequestCommand;
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
            if (usersInPoll.getOrDefault(chatId, false)) {
                // Если да - передаем ответ в PollService
                documentRequestCommand.processPollAnswer(this, chatId, messageText);
                return;
            }
            Boolean showMainKeyboard = userShowMainKeyboard.getOrDefault(chatId, true);

            if (messageText.equals("/start")) {
                startCommand.execute(this, chatId);
                userShowMainKeyboard.put(chatId, true);
                sendMainMenu(chatId);
            } else if (showMainKeyboard) {
                // Получаем английскую команду из русского названия
                String command = commandMap.get(messageText);
                if (command == null) {
                    // Если команда не найдена в commandMap, то, возможно, это прямая английская команда
                    command = messageText; // Используем messageText как команду
                }
                // Обработка команд с основной клавиатуры
                if (command != null) {
                    switch (command) {
                        case HELP_COMMAND:
                            helpCommand.execute(this, chatId);
                            break;
                        case DOCS_COMMAND:
                            docsCommand.execute(this, chatId);
                            break;
                        case DORMITORY_COMMAND:
                            dormitoryCommand.execute(this, chatId);
                            break;
                        case CATHEDRA_COMMAND:
                            cathedraCommand.showCathedraList(this, chatId); // Вызываем метод показа списка кафедр
                            userShowMainKeyboard.put(chatId, false); // Скрываем основную клавиатуру
                            break;
                        case DIRECTION_COMMAND:
                            directionCommand.showDirectionList(this, chatId); // Вызываем метод показа списка направлений
                            userShowMainKeyboard.put(chatId, false); // Скрываем основную клавиатуру
                            break;
                        case "/back":  // Обработка "Назад"
                            userShowMainKeyboard.put(chatId, true);
                            sendMainMenu(chatId);
                            break;
                        case SUBMIT_DOCUMENTS:
                            usersInPoll.put(chatId, true); // Устанавливаем флаг опроса
                            documentRequestCommand.startPoll(this, chatId);
                            break;
                        default:
                            sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
                    }
                }
            } else {
                // Обработка команд, когда основная клавиатура скрыта (например, "Назад")
                if (messageText.equals("Назад")) {
                    userShowMainKeyboard.put(chatId, true);
                    sendMainMenu(chatId); // Отображаем основное меню
                } else {
                    // Предполагаем, что это название кафедры/направления
                    Cathedra cathedra = cathedraCommand.getCathedraBySecondName(messageText); // Поменял здесь
                    if (cathedra != null) {
                        sendMessage(chatId, "Информация о кафедре \"" + cathedra.getSecondName() + "\":\n" + cathedra.getDescription());
                    } else {
                        Direction direction = directionCommand.getDirectionBySecondName(messageText); // И здесь
                        if (direction != null) {
                            sendMessage(chatId, "Информация о направлении \"" + direction.getName() + "\":\n" + direction.getDescription());
                        } else {
                            sendMessage(chatId, "Неизвестная команда.");
                        }
                    }
                }
            }
        }
    }

    private void sendMainMenu(long chatId) { // Создаем основную клавиатуру
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Список команд")); // Заменили /help на "Помощь"
        row1.add(new KeyboardButton("Документы")); // Заменили /docs на "Документы"
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Кафедры")); // Заменили /cathedra на "Кафедры"
        row2.add(new KeyboardButton("Направления"));
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Общежитие"));
        row3.add(new KeyboardButton("Подать документы"));// Заменили /dormitory на "Общежитие"

        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboardRows.add(row3);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
    public void finishPoll(long chatId) {
        usersInPoll.remove(chatId); // Четко сбрасываем флаг
        sendMainMenu(chatId); // Возвращаем главное меню
    }
}