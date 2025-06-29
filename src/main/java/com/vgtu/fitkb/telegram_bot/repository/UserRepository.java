package com.vgtu.fitkb.telegram_bot.repository;

import com.vgtu.fitkb.telegram_bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByLastName(String lastName);

    List<User> findByPointsGreaterThanEqual(Integer minPoints);

    Optional<User> findByChatId(Long chatId);
    void deleteByChatId(Long chatId);
}