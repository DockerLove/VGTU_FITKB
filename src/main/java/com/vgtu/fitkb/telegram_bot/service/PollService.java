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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

            "Есть ли у вас какие либо льготы?(Да/Нет)",

            "1.Являющимся детьми-сиротами и детьми, оставшимися без попечения " +
                    "родителей, лицами из числа детей-сирот и детей, оставшихся без " +
                    "попечения родителей, лицами, потерявшими в период обучения обоих " +
                    "родителей или единственного родителя? (Да/Нет)",
            "2.Являющимися детьми-инвалидами, инвалидами I и II групп, " +
                    "инвалидами с детства? (Да/Нет)",
            "3.Подвергшимся воздействию радиации вследствие катастрофы на " +
                    "Чернобыльской АЭС и иных радиационных катастроф, вследствие " +
                    "ядерных испытаний на Семипалатинском полигоне? (Да/Нет)",
            "4.Являющимся инвалидами вследствие военной травмы или заболевания, " +
                    "полученных в период прохождения военной службы, и ветеранами " +
                    "боевых действии, а также студентам из числа граждан, проходивших в " +
                    "течение не менее трех лет военную службу по контракту на воинских " +
                    "должностях, подлежащих замещению солдатами, матросами, " +
                    "сержантами, старшинами, и уволенных с военной службы по " +
                    "основаниям, предусмотренным подпунктами «б» - «г» пункта 1, " +
                    "подпунктом «а» пункта 2 и подпунктами «а» - «в» пункта 3 статьи 51 " +
                    "Федерального закона «О воинской обязанности и военной службе»? (Да/Нет)",

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
            case 0: // Обработка ФИО (без изменений)
                String[] parts = answer.split(" ");
                if (parts.length >= 2) {
                    boolean isValid = true;
                    for (String part : parts) {
                        if (!Pattern.matches("^[\\p{L}-'.]+$", part)) {
                            isValid = false;
                            break;
                        }
                    }

                    if (isValid) {
                        state.user.setSecondName(parts[0]);
                        state.user.setFirstName(parts[1]);
                        if (parts.length == 3) {
                            state.user.setLastName(parts[2]);
                        } else if (parts.length > 3) {
                            state.user.setLastName(String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)));
                        }
                        state.questionNumber++;
                        askQuestion(bot, chatId, state);
                    } else {
                        sendMessage(bot, chatId, "Пожалуйста, введите ФИО, используя только буквы и допустимые символы (-, ', .). Цифры не допускаются.");
                    }
                } else {
                    sendMessage(bot, chatId, "Пожалуйста, введите ФИО правильно (Фамилия Имя Отчество(если есть)).");
                }
                break;

            case 1: // Дата рождения (без изменений)
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

            case 2: // Баллы ЕГЭ (без изменений)
                try {
                    int scores = Integer.parseInt(answer);
                    if (scores < 0 || scores > 315) {
                        sendMessage(bot, chatId, "Пожалуйста, введите корректную сумму баллов (от 0 до 315).");
                        return;
                    }
                    state.user.setAvgScores(scores);
                    state.questionNumber++;
                    askQuestion(bot, chatId, state);
                } catch (NumberFormatException e) {
                    sendMessage(bot, chatId, "Пожалуйста, введите число (сумму баллов за 3 предмета).");
                }
                break;

            case 3: // Вопрос о наличии льгот
                if (answer.equalsIgnoreCase("нет")) {
                    finishPoll(bot, chatId, state); // Завершаем опрос если нет льгот
                } else if (answer.equalsIgnoreCase("да")) {
                    // Создаем сообщение с клавиатурой выбора льгот
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText("Выберите номер вашей льготы:");

                    // Настройка клавиатуры с выбором льгот 1-4
                    ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                    keyboard.setResizeKeyboard(true);
                    keyboard.setOneTimeKeyboard(true);

                    List<KeyboardRow> keyboardRows = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("1");
                    row.add("2");
                    row.add("3");
                    row.add("4");
                    keyboardRows.add(row);

                    // Добавляем кнопки управления
                    KeyboardRow controlRow = new KeyboardRow();
                    controlRow.add("Назад");
                    controlRow.add("Отмена");
                    keyboardRows.add(controlRow);

                    keyboard.setKeyboard(keyboardRows);
                    message.setReplyMarkup(keyboard);

                    try {
                        bot.execute(message); // Отправляем сообщение с клавиатурой
                        state.questionNumber++; // Переходим к обработке выбора льготы
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                        sendMessage(bot, chatId, "Произошла ошибка при отправке сообщения");
                    }
                } else {
                    sendMessage(bot, chatId, "Пожалуйста, ответьте 'Да' или 'Нет'.");
                }
                break;

            case 4: // Обработка выбора льготы (1-4)
                if (answer.matches("[1-4]")) {
                    // Сохраняем тип льготы (если нужно)
                    // Пропускаем вопросы 5-8 (индексы 4-7), переходим сразу к вопросу 9 (индекс 8)
                    state.questionNumber = 8;
                    askQuestion(bot, chatId, state);
                } else {
                    sendMessage(bot, chatId, "Пожалуйста, выберите номер льготы от 1 до 4.");
                }
                break;

            default: // Обработка вопросов 9-13
                if (answer.equalsIgnoreCase("да") || answer.equalsIgnoreCase("нет")) {
                    if (answer.equalsIgnoreCase("да")) {
                        // Начисляем баллы за дополнительные льготы
                        state.user.setPoints(state.user.getPoints() + 1);
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

    // Метод askQuestion с учетом новой логики
    private void askQuestion(VGUTelegramBot bot, Long chatId, UserPollState state) {
        String question = questions.get(state.questionNumber);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(question);

        // Для вопроса о льготах (индекс 3) используем стандартную клавиатуру Да/Нет
        if (state.questionNumber == 3) {
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            List<KeyboardRow> keyboardRows = new ArrayList<>();

            KeyboardRow answerRow = new KeyboardRow();
            answerRow.add("Да");
            answerRow.add("Нет");
            keyboardRows.add(answerRow);

            KeyboardRow controlRow = new KeyboardRow();
            if (state.questionNumber > 0) controlRow.add("Назад");
            controlRow.add("Отмена");
            keyboardRows.add(controlRow);

            keyboardMarkup.setKeyboard(keyboardRows);
            keyboardMarkup.setResizeKeyboard(true);
            message.setReplyMarkup(keyboardMarkup);
        }
        // Для остальных вопросов (кроме выбора льготы) используем стандартную клавиатуру
        else if (state.questionNumber != 4) {
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            List<KeyboardRow> keyboardRows = new ArrayList<>();

            if (state.questionNumber >= 3) { // Для вопросов Да/Нет
                KeyboardRow answerRow = new KeyboardRow();
                answerRow.add("Да");
                answerRow.add("Нет");
                keyboardRows.add(answerRow);
            }

            KeyboardRow controlRow = new KeyboardRow();
            if (state.questionNumber > 0) controlRow.add("Назад");
            controlRow.add("Отмена");
            keyboardRows.add(controlRow);

            keyboardMarkup.setKeyboard(keyboardRows);
            keyboardMarkup.setResizeKeyboard(true);
            message.setReplyMarkup(keyboardMarkup);
        }

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleBackCommand(VGUTelegramBot bot, Long chatId, UserPollState state) {
        if (state.questionNumber > 0) {
            // Откат баллов при возврате
            if (state.questionNumber >= 8 && state.questionNumber <= 12) { // Вопросы 9-13
                // Убираем 1 балл, если был ответ "Да" на предыдущий вопрос
                if (state.user.getPoints() > 0) {
                    state.user.setPoints(state.user.getPoints() - 1);
                }
            }

            // Особый случай - возврат с выбора льготы (вопрос 5)
            if (state.questionNumber == 4) {
                // Сбрасываем выбор льготы
                // Возвращаемся к вопросу о наличии льгот (вопрос 4)
                state.questionNumber = 3;
            } else {
                // Стандартный возврат на предыдущий вопрос
                state.questionNumber--;
            }

            // Для возврата к выбору льгот создаем сообщение с клавиатурой
            if (state.questionNumber == 3) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId.toString());
                message.setText("Выберите номер вашей льготы:");

                ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                keyboard.setResizeKeyboard(true);
                keyboard.setOneTimeKeyboard(true);

                List<KeyboardRow> rows = new ArrayList<>();
                KeyboardRow row = new KeyboardRow();
                row.add("1");
                row.add("2");
                row.add("3");
                row.add("4");
                rows.add(row);

                // Добавляем кнопки управления
                KeyboardRow controlRow = new KeyboardRow();
                controlRow.add("Назад");
                controlRow.add("Отмена");
                rows.add(controlRow);

                keyboard.setKeyboard(rows);
                message.setReplyMarkup(keyboard);

                try {
                    bot.execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                askQuestion(bot, chatId, state);
            }
        } else {
            sendMessage(bot, chatId, "Вы в начале опроса, нельзя вернуться назад.");
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