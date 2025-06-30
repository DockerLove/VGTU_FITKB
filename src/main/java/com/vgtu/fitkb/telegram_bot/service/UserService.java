package com.vgtu.fitkb.telegram_bot.service;
import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.model.User;
import com.vgtu.fitkb.telegram_bot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final GoogleSheetsService googleSheetsService;
    private final UserRepository userRepository;
    @Autowired
    public UserService(GoogleSheetsService googleSheetsService,UserRepository userRepository) {
        this.googleSheetsService = googleSheetsService;
        this.userRepository = userRepository;
    }

    @Transactional
    public void saveUser(User user) {
        try {
            User user1 = userRepository.save(user);
            googleSheetsService.addPersonToSheet(user1);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }


    @Transactional
    public void deleteUserByChatId(User user) throws Exception {
        userRepository.deleteByChatId(user.getChatId());
        googleSheetsService.deleteRowByUserId(user.getId().toString());
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

    public User findByChatId(Long chatId){
        return userRepository.findByChatId(chatId);
    }


}