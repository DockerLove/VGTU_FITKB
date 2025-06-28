package com.vgtu.fitkb.telegram_bot.service;
import com.vgtu.fitkb.telegram_bot.model.User;
import com.vgtu.fitkb.telegram_bot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}