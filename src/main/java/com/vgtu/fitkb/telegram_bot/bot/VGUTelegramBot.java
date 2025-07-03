package com.vgtu.fitkb.telegram_bot.bot;

import com.vgtu.fitkb.telegram_bot.command.*;
import com.vgtu.fitkb.telegram_bot.model.Cathedra;
import com.vgtu.fitkb.telegram_bot.model.Direction;
import com.vgtu.fitkb.telegram_bot.model.User;
import com.vgtu.fitkb.telegram_bot.service.SubmitService;
import com.vgtu.fitkb.telegram_bot.service.UserService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import com.vgtu.fitkb.telegram_bot.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final SubmitService submitService;
    private final UserService userService;
    private final Map<Long, Boolean> usersInPoll = new HashMap<>();
    private final Map<Long, Boolean> usersUploadingFiles = new ConcurrentHashMap<>();

    private static final String HELP_COMMAND = "/help";
    private static final String DOCS_COMMAND = "/docs";
    private static final String DORMITORY_COMMAND = "/dormitory";
    private static final String CATHEDRA_COMMAND = "/cathedra";
    private static final String DIRECTION_COMMAND = "/direction";
    private static final String SUBMIT_DOCUMENTS = "/submit";
    private static final String RATING_COMMAND = "/rating";

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
        commandMap.put("Рейтинг", RATING_COMMAND);
    }
    @Autowired
    public VGUTelegramBot(BotConfig config, StartCommand startCommand, HelpCommand helpCommand,
                          CathedraCommand cathedraCommand, DirectionCommand directionCommand,
                          DormitoryCommand dormitoryCommand, DocsCommand docsCommand, DocumentRequestCommand documentRequestCommand,
                          SubmitService submitService, UserService userService) {
        this.config = config;
        this.startCommand = startCommand;
        this.helpCommand = helpCommand;
        this.cathedraCommand = cathedraCommand;
        this.directionCommand = directionCommand;
        this.dormitoryCommand = dormitoryCommand;
        this.docsCommand = docsCommand;
        this.documentRequestCommand = documentRequestCommand;
        this.submitService = submitService;
        this.userService = userService;
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
        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        long chatId = message.getChatId();

        // 1. Обработка документов (вынесено в начало как приоритетное действие)
        if (message.hasDocument()) {
            handleDocumentUpload(chatId, message.getDocument());
            return;
        }

        // 2. Обработка текстовых сообщений
        if (message.hasText()) {
            String text = message.getText();

            // Приоритетная обработка состояний
            if (usersUploadingFiles.getOrDefault(chatId, false)) {
                try {
                    handleFileUploadCommands(chatId, text);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            if (usersInPoll.getOrDefault(chatId, false)) {
                documentRequestCommand.processPollAnswer(this, chatId, text);
                return;
            }

            // Обработка команды /start
            if (text.equals("/start")) {
                startCommand.execute(this, chatId);
                userShowMainKeyboard.put(chatId, true);
                sendMainMenu(chatId);
                return;
            }

            // Обработка остальных команд
            if (userShowMainKeyboard.getOrDefault(chatId, true)) {
                handleMainMenuCommands(chatId, text);
            } else {
                handleSecondaryMenuCommands(chatId, text);
            }
        }
    }

    private void handleMainMenuCommands(long chatId, String text) {
        String command = commandMap.getOrDefault(text, text);
        switch (command) {
            case RATING_COMMAND -> userService.showRating(this,chatId);
            case HELP_COMMAND -> helpCommand.execute(this, chatId);
            case DOCS_COMMAND -> docsCommand.execute(this, chatId);
            case DORMITORY_COMMAND -> dormitoryCommand.execute(this, chatId);
            case CATHEDRA_COMMAND -> {
                cathedraCommand.showCathedraList(this, chatId);
                userShowMainKeyboard.put(chatId, false);
            }
            case DIRECTION_COMMAND -> {
                directionCommand.showDirectionList(this, chatId);
                userShowMainKeyboard.put(chatId, false);
            }
            case SUBMIT_DOCUMENTS -> {
                usersInPoll.put(chatId, true);
                documentRequestCommand.startPoll(this, chatId);
            }
            default -> sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
        }
    }

    private void handleSecondaryMenuCommands(long chatId, String text) {
        if (text.equals("Назад")) {
            userShowMainKeyboard.put(chatId, true);
            sendMainMenu(chatId);
            return;
        }

        Cathedra cathedra = cathedraCommand.getCathedraBySecondName(text);
        if (cathedra != null) {
            sendMessage(chatId, "Информация о кафедре \"" + cathedra.getSecondName() + "\":\n" + cathedra.getDescription());
            return;
        }

        Direction direction = directionCommand.getDirectionBySecondName(text);
        if (direction != null) {
            sendMessage(chatId, "Информация о направлении \"" + direction.getName() + "\":\n" + direction.getDescription());
            return;
        }

        sendMessage(chatId, "Неизвестная команда. Нажмите «Назад» для возврата в меню.");
    }

    private void handleDocumentUpload(long chatId, Document document) {
        try {
            if (!isValidDocument(document)) {
                sendMessage(chatId, "❌ Принимаем только PDF/JPG/PNG до 5MB");
                return;
            }

            // Создаем Update для передачи в processDocument
            Update dummyUpdate = createDocumentUpdate(chatId, document);

            // Вызываем метод с правильными параметрами
            documentRequestCommand.processDocument(this, dummyUpdate);

            sendFileUploadKeyboard(chatId, "✅ Файл " + document.getFileName() + " принят. Отправьте следующий или нажмите «Готово»");

        } catch (Exception e) {
            sendMessage(chatId, "⚠️ Ошибка загрузки файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Update createDocumentUpdate(long chatId, Document document) {
        Update update = new Update();
        Message message = new Message();

        // Настраиваем сообщение с документом
        message.setChat(new Chat(chatId, "private"));
        message.setDocument(document);

        update.setMessage(message);
        return update;
    }

    private boolean isValidDocument(Document doc) {
        String fileName = doc.getFileName().toLowerCase();
        return fileName.matches(".*\\.(pdf|jpg|jpeg|png)$") && doc.getFileSize() <= 5_000_000;
    }

    private void handleFileUploadCommands(long chatId, String command){
        // Проверяем, находится ли пользователь в режиме загрузки
        if (!usersUploadingFiles.getOrDefault(chatId, false)) {
            sendMainMenu(chatId);
            return;
        }

        switch (command) {
            case "Готово":
                try {
                    boolean success = documentRequestCommand.completeSubmission(this, chatId);

                    if (success) {
                        usersUploadingFiles.put(chatId, false); // Выходим из режима загрузки
                        sendMainMenu(chatId);
                    } else {
                        // Остаемся в режиме загрузки, но просим отправить файлы
                        sendFileUploadKeyboard(chatId,
                                "Вы не отправили ни одного файла. Пожалуйста, отправьте документы или нажмите «Отмена»");
                    }
                }catch (Exception e){
                    usersUploadingFiles.put(chatId, false);
                    sendMessage(chatId,"Ошибка: C одного аккаунта максимум 1 заявление");
                    sendMainMenu(chatId);
                }
                break;

            case "Отмена":
                try {
                    User user = userService.findByChatId(chatId);
                    userService.deleteUserByChatId(user);
                    usersUploadingFiles.put(chatId, false);
                    documentRequestCommand.cancelSubmission(this, chatId);
                    sendMainMenu(chatId);
                }catch (Exception e){
                    sendMainMenu(chatId);
                }
                break;

            default:
                sendFileUploadKeyboard(chatId, "Отправьте документ или используйте кнопки:");
        }
    }


    private void handleRegularCommands(long chatId, String text) {
        if (text.equals("/start")) {
            startCommand.execute(this, chatId);
            userShowMainKeyboard.put(chatId, true);
            sendMainMenu(chatId);
            return;
        }

        if (userShowMainKeyboard.getOrDefault(chatId, true)) {
            String command = commandMap.getOrDefault(text, text);
            switch (command) {
                case HELP_COMMAND -> helpCommand.execute(this, chatId);
                case DOCS_COMMAND -> docsCommand.execute(this, chatId);
                case DORMITORY_COMMAND -> dormitoryCommand.execute(this, chatId);
                case CATHEDRA_COMMAND -> {
                    cathedraCommand.showCathedraList(this, chatId);
                    userShowMainKeyboard.put(chatId, false);
                }
                case DIRECTION_COMMAND -> {
                    directionCommand.showDirectionList(this, chatId);
                    userShowMainKeyboard.put(chatId, false);
                }
                case "/back" -> {
                    userShowMainKeyboard.put(chatId, true);
                    sendMainMenu(chatId);
                }
                case SUBMIT_DOCUMENTS -> {
                    usersInPoll.put(chatId, true);
                    documentRequestCommand.startPoll(this, chatId);
                }
                default -> sendMessage(chatId, "Неизвестная команда. Используйте /help");
            }
        } else {
            if (text.equals("Назад")) {
                userShowMainKeyboard.put(chatId, true);
                sendMainMenu(chatId);
            } else {
                handleCathedraOrDirectionQuery(chatId, text);
            }
        }
    }

    private void handleCathedraOrDirectionQuery(long chatId, String query) {
        Cathedra cathedra = cathedraCommand.getCathedraBySecondName(query);
        if (cathedra != null) {
            sendMessage(chatId, cathedra.getDescription());
            return;
        }

        Direction direction = directionCommand.getDirectionBySecondName(query);
        if (direction != null) {
            sendMessage(chatId, direction.getDescription());
            return;
        }

        sendMessage(chatId, "Неизвестный запрос. Используйте кнопки меню.");
    }


    public void sendMainMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(createKeyboardRow("Список команд", "Документы"));
        rows.add(createKeyboardRow("Кафедры", "Направления"));
        rows.add(createKeyboardRow("Общежитие", "Подать документы"));
        rows.add(createKeyboardRow("Рейтинг"));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message);
    }

    private void sendFileUploadKeyboard(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setKeyboard(List.of(createKeyboardRow("Готово", "Отмена")));

        message.setReplyMarkup(keyboard);
        executeMessage(message);
    }

    private KeyboardRow createKeyboardRow(String... buttons) {
        KeyboardRow row = new KeyboardRow();
        for (String button : buttons) {
            row.add(new KeyboardButton(button));
        }
        return row;
    }

    private void sendMessage(long chatId, String text) {
        executeMessage(new SendMessage(String.valueOf(chatId), text));
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void startFileUpload(long chatId) {
        usersUploadingFiles.put(chatId, true);
        sendFileUploadKeyboard(chatId, "📎 Отправляйте документы по одному и НЕ в сжатом виде, если это JPG или PNG. Когда закончите, нажмите «Готово»");
    }

    public void finishPoll(long chatId) {
        usersInPoll.remove(chatId);
        startFileUpload(chatId); // Переходим к загрузке файлов после опроса
    }
    public void cancelPoll(long chatId){
        usersInPoll.remove(chatId);
        sendMainMenu(chatId);
    }
}