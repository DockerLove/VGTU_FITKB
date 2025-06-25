package com.vgtu.fitkb.telegram_bot.command;

import com.vgtu.fitkb.telegram_bot.bot.VGUTelegramBot;
import com.vgtu.fitkb.telegram_bot.model.Direction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class DormitoryCommand {
    public void execute(VGUTelegramBot bot, long chatId) {
        String dormitory = "Общежития:\n\n" +
                "Студенческий городок ВГТУ располагает 7 общежитиями, в которых проживают иногородние и иностранные студенты. Общежития рассчитаны на проживание 3100 человек, в том числе предусмотрены отдельные секции для проживания иностранных студентов, а также для семейных иногородних обучающихся.\n" +
                "\nСуществует возможность предоставления временного проживания для студентов заочной формы обучения, абитуриентов - на период прохождения вступительных экзаменов.\n" +
                "\nОдним из важнейших событий в 2022 году стало торжественное открытие нового общежития квартирного типа, по адресу ул. 20-летия Октября, 79 корпус 1. Общежитие полностью соответствует передовым стандартам университетских кампусов. Имеется оборудование для маломобильных граждан, залы для занятий спортом на каждом этаже, комнаты для коллективной работы студентов, современная прачечная (цокольный этаж), комнаты для лиц с ограниченными возможностями здоровья.\n" +
                "\nАдреса общежитий: \n" +
                "г. Воронеж, Московский проспект, д. 179, общежитие №3\n" +
                "г. Воронеж, Московский проспект, д. 179, общежитие №4\n" +
                "г. Воронеж, ул.20-летия Октября, дом 77, общежитие №3\n" +
                "г. Воронеж, ул.20-летия Октября, дом 75, общежитие №4\n" +
                "г. Воронеж, ул.20-летия Октября, дом 77А, общежитие №5\n" +
                "г. Воронеж, ул.20-летия Октября, дом 81, общежитие №6\n" +
                "г. Воронеж, ул.20-летия Октября, дом 79 кор.1, общежитие №7\n";

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(dormitory);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
