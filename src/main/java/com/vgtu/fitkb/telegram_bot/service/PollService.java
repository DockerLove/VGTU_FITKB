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
            "Ваше имя, фамилия, отчество?",
            "Ваша дата рождения (в формате ДД.ММ.ГГГГ)?",
            "Вы являетесь ребенком-сиротой или остались без попечения родителей? (Да/Нет)",
            "Вам установлена I или II группа инвалидности, либо вы являетесь ребенком-инвалидом? (Да/Нет)",
            "Вы пострадали от радиационных катастроф (Чернобыль, Семипалатинск)? (Да/Нет)",
            "Вы являетесь ветераном боевых действий или инвалидом вследствие военной травмы? (Да/Нет)",
            "Вы проходили длительную военную службу по контракту? (Да/Нет)"
    );

    public void startPoll(VGUTelegramBot bot, Long chatId) {
        UserPollState state = new UserPollState();
        userStates.put(chatId, state);
        askQuestion(bot, chatId, state);
    }

    public void processAnswer(VGUTelegramBot bot, Long chatId, String answer) {
        UserPollState state = userStates.get(chatId);
        if (state == null) {
            sendMessage(bot,chatId, "Опрос не начат. Введите /submit для начала.");
            return;
        }
        if (state.questionNumber == 0) {
            // Обработка ФИО
            String[] parts = answer.split(" ");
            if (parts.length == 3) {
                state.user.setFirstName(parts[0]);
                state.user.setSecondName(parts[1]);
                state.user.setLastName(parts[2]);
                state.questionNumber++;
                askQuestion(bot, chatId,state);
            } else {
                sendMessage(bot, chatId, "Пожалуйста, введите ФИО полностью (Имя Фамилия Отчество).");
            }
        } else if (state.questionNumber == 1) {
            // Обработка даты рождения
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            try {
                LocalDate birthday = LocalDate.parse(answer, formatter);
                state.user.setBirthday(birthday);
                state.questionNumber++;
                askQuestion(bot, chatId,state);
            } catch (DateTimeParseException e) {
                sendMessage(bot, chatId, "Пожалуйста, введите дату рождения в формате ДД.ММ.ГГГГ.");
            }
        } else if (state.questionNumber > 1 && state.questionNumber < questions.size()) {
            // Обработка вопросов Да/Нет
            if (answer.equalsIgnoreCase("да")) {
                // Начисляем баллы в зависимости от вопроса
                switch (state.questionNumber) {
                    case 2:
                        state.user.setPoints(state.user.getPoints() + 5);
                        break;
                    case 3:
                        state.user.setPoints(state.user.getPoints() + 4);
                        break;
                    case 4:
                        state.user.setPoints(state.user.getPoints() + 3);
                        break;
                    case 5:
                        state.user.setPoints(state.user.getPoints() + 2);
                        break;
                    case 6:
                        state.user.setPoints(state.user.getPoints() + 1);
                        break;
                    // Другие вопросы...
                    default:
                        break;
                }
                state.questionNumber++;
                if(state.questionNumber < questions.size()){
                    askQuestion(bot, chatId,state);
                }else {
                    finishPoll(bot,chatId,state);
                }

            } else if (answer.equalsIgnoreCase("нет")) {
                state.questionNumber++;
                if(state.questionNumber < questions.size()){
                    askQuestion(bot, chatId,state);
                }else {
                    finishPoll(bot,chatId,state);
                }
            } else {
                sendMessage(bot, chatId, "Пожалуйста, отвечайте только 'Да' или 'Нет'.");
            }
        }
    }


    private void askQuestion(VGUTelegramBot bot, Long chatId, UserPollState state) {
        String question = questions.get(state.questionNumber);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(question);

        if (state.questionNumber > 1) {
            message.setReplyMarkup(createYesNoKeyboard());
        }

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createYesNoKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton yesButton = new KeyboardButton("Да");
        KeyboardButton noButton = new KeyboardButton("Нет");
        row.add(yesButton);
        row.add(noButton);
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true); //  уменьшить размер клавиатуры
        return keyboardMarkup;
    }

    private void finishPoll(VGUTelegramBot bot, Long chatId, UserPollState state) {
        state.user.setChatId(chatId);
        userService.saveUser(state.user);
        userStates.remove(chatId);
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
}