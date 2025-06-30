package com.vgtu.fitkb.telegram_bot.service;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PollService {

    private final UserService userService;
    private final Map<Long, UserPollState> userStates = new ConcurrentHashMap<>();

    private static class UserPollState {
        User user = new User();
        int questionNumber = 0;
    }

    @Autowired
    public PollService(UserService userService) {
        this.userService = userService;
    }

    private final List<String> questions = List.of(
            "Ваше фамилия, имя, отчество?",
            "Ваша дата рождения (в формате ДД.ММ.ГГГГ)?",
            "Введите сумму баллов за ЕГЭ за 3 предмета, с которыми поступали:",
            "Вы являетесь ребенком-сиротой или остались без попечения родителей? (Да/Нет)",
            "Вам установлена I или II группа инвалидности, либо вы являетесь ребенком-инвалидом? (Да/Нет)",
            "Вы пострадали от радиационных катастроф (Чернобыль, Семипалатинск)? (Да/Нет)",
            "Вы являетесь ветераном боевых действий или инвалидом вследствие военной травмы? (Да/Нет)",
            "Вы проходили длительную военную службу по контракту? (Да/Нет)",
            "Вы студент из многодетной семьи (3 и более детей)? (Да/Нет)",
            "Студенты, воспитывающиеся в неполных семьях (один родитель)? (Да/Нет)",
            "Студенты, прибывшие из отдаленных или труднодоступных регионов (Крайний Север, Дальний Восток, сельские территории с низкой инфраструктурой)? (Да/Нет)",
            "Студенты, чьи родители являются инвалидами I или II группы? (Да/Нет)",
            "Студенты — родители, воспитывающие ребенка в одиночку? (Да/Нет)"
    );

    public void startPoll(VGUTelegramBot bot, Long chatId) {
        UserPollState state = new UserPollState();
        userStates.put(chatId, state);
        askQuestion(bot, chatId, state);
    }

    public void processAnswer(VGUTelegramBot bot, Long chatId, String answer) throws Exception {
        UserPollState state = userStates.get(chatId);
        if (state == null) {
            sendMessage(bot, chatId, "Опрос не начат. Введите /submit для начала.");
            return;
        }

        if (answer.equalsIgnoreCase("Отмена")) {
            cancelPoll(bot, chatId, state);
            return;
        } else if (answer.equalsIgnoreCase("Назад")) {
            handleBackCommand(bot, chatId, state);
            return;
        }

        switch (state.questionNumber) {
            case 0:
                // Обработка ФИО (без изменений)
                String[] parts = answer.split(" ");
                if (parts.length == 3) {
                    state.user.setFirstName(parts[1]);
                    state.user.setSecondName(parts[0]);
                    state.user.setLastName(parts[2]);
                    state.questionNumber++;
                    askQuestion(bot, chatId, state);
                } else {
                    sendMessage(bot, chatId, "Пожалуйста, введите ФИО полностью (Фамилия Имя Отчество).");
                }
                break;

            case 1:
                // Обработка даты рождения (без изменений)
                try {
                    LocalDate birthday = LocalDate.parse(answer, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    LocalDate now = LocalDate.now();
                    LocalDate minDate = LocalDate.of(1900, 1, 1);

                    if (birthday.isAfter(now)) {
                        sendMessage(bot, chatId, "Дата рождения не может быть в будущем. Введите корректную дату.");
                        return;
                    }

                    if (birthday.isBefore(minDate)) {
                        sendMessage(bot, chatId, "Дата рождения не может быть раньше 1900 года. Введите корректную дату.");
                        return;
                    }

                    state.user.setBirthday(birthday);
                    state.questionNumber++;
                    askQuestion(bot, chatId, state);
                } catch (DateTimeParseException e) {
                    sendMessage(bot, chatId, "Пожалуйста, введите дату рождения в формате ДД.ММ.ГГГГ.");
                }
                break;

            case 2:
                // Обработка баллов ЕГЭ (без изменений)
                try {
                    int scores = Integer.parseInt(answer);
                    if (scores < 0 || scores > 300) {
                        sendMessage(bot, chatId, "Пожалуйста, введите корректную сумму баллов (от 0 до 300).");
                        return;
                    }
                    state.user.setAvgScores(scores);
                    state.questionNumber++;
                    askQuestion(bot, chatId, state);
                } catch (NumberFormatException e) {
                    sendMessage(bot, chatId, "Пожалуйста, введите число (сумму баллов за 3 предмета).");
                }
                break;

            default:
                if (answer.equalsIgnoreCase("да") || answer.equalsIgnoreCase("нет")) {
                    if (answer.equalsIgnoreCase("да")) {
                        // Начисление баллов в зависимости от номера вопроса
                        if (state.questionNumber >= 3 && state.questionNumber <= 7) {
                            state.user.setPoints(state.user.getPoints() + 2); // +2 балла за вопросы 3-7
                        } else if (state.questionNumber >= 8 && state.questionNumber <= 12) {
                            state.user.setPoints(state.user.getPoints() + 1); // +1 балл за вопросы 8-12
                        }
                    }

                    state.questionNumber++;
                    if (state.questionNumber < questions.size()) {
                        askQuestion(bot, chatId, state);
                    } else {
                        finishPoll(bot, chatId, state);
                    }
                } else {
                    sendMessage(bot, chatId, "Пожалуйста, отвечайте только 'Да' или 'Нет'.");
                }
                break;
        }
    }

    // ... (остальные методы остаются без изменений до handleBackCommand)

    private void handleBackCommand(VGUTelegramBot bot, Long chatId, UserPollState state) {
        if (state.questionNumber > 0) {
            // Откат баллов при возврате
            if (state.questionNumber >= 3 && state.questionNumber <= 7) {
                state.user.setPoints(state.user.getPoints() - 2); // -2 балла за вопросы 3-7
            } else if (state.questionNumber >= 8 && state.questionNumber <= 12) {
                state.user.setPoints(state.user.getPoints() - 1); // -1 балл за вопросы 8-12
            }

            state.questionNumber--;
            askQuestion(bot, chatId, state);
        } else {
            sendMessage(bot, chatId, "Вы в начале опроса, нельзя вернуться назад.");
        }
    }

    private void askQuestion(VGUTelegramBot bot, Long chatId, UserPollState state) {
        String question = questions.get(state.questionNumber);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(question);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Для вопросов Да/Нет (начиная с 3-го)
        if (state.questionNumber >= 3) {
            KeyboardRow answerRow = new KeyboardRow();
            answerRow.add("Да");
            answerRow.add("Нет");
            keyboardRows.add(answerRow);
        }

        // Кнопки управления
        KeyboardRow controlRow = new KeyboardRow();
        if (state.questionNumber > 0) {
            controlRow.add("Назад");
        }
        controlRow.add("Отмена");
        keyboardRows.add(controlRow);

        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void finishPoll(VGUTelegramBot bot, Long chatId, UserPollState state) throws Exception {
        state.user.setChatId(chatId);
        userService.saveUser(state.user);
        userStates.remove(chatId);
        sendMessage(bot, chatId, "Опрос завершен! Спасибо за ответы.");
        bot.finishPoll(chatId);
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

    private void cancelPoll(VGUTelegramBot bot, Long chatId, UserPollState state) {
        userStates.remove(chatId);
        sendMessage(bot, chatId, "Опрос отменён. Все введённые данные удалены.");
        bot.cancelPoll(chatId);
    }
}