package com.vgtu.fitkb.telegram_bot.service;
import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.model.User;
import com.vgtu.fitkb.telegram_bot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void updateUserPoints(Long userId, Integer additionalPoints) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPoints(user.getPoints() + additionalPoints);
            userRepository.save(user);
        });
    }
    @Transactional
    public void deleteUserByChatId(long chatId) {
        userRepository.deleteByChatId(chatId);
    }

    public void showRating(VGUTelegramBot bot, long chatId) {
        List<User> users = getAllUsersOrderedByPoints();

        if (users.isEmpty()) {
            sendMessage(bot,chatId, "Рейтинг пока пуст.");
            return;
        }

        StringBuilder ratingMessage = new StringBuilder("   Рейтинг абитуриентов:\n");

        int position = 1;
        for (User user : users) {
            ratingMessage.append(String.format(
                    "%d. %s %s %s (%s)\n",
                    position++,
                    user.getLastName(),
                    user.getFirstName(),
                    user.getSecondName() != null ? user.getSecondName() : "",
                    user.getBirthday() != null ? user.getBirthday().toString() : "дата не указана"
            ));
        }

        sendMessage(bot,chatId, ratingMessage.toString());
    }

    public List<User> getAllUsersOrderedByPoints() {
        return userRepository.findAllByOrderByPointsDescRegistrationDateAsc();
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

    public Optional<User> findByChatId(Long chatId){
        return userRepository.findByChatId(chatId);
    }
}