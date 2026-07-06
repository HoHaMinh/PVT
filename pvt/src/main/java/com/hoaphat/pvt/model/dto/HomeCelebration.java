package com.hoaphat.pvt.model.dto;

import com.hoaphat.pvt.model.event.SpecialMessage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeCelebration {
    private final LocalDate date;
    private final List<String> birthdayNames;
    private final List<SpecialMessage> specialMessages;

    public HomeCelebration(LocalDate date, List<String> birthdayNames,
                           List<SpecialMessage> specialMessages) {
        this.date = date;
        this.birthdayNames = birthdayNames == null
                ? new ArrayList<>() : new ArrayList<>(birthdayNames);
        this.specialMessages = specialMessages == null
                ? new ArrayList<>() : new ArrayList<>(specialMessages);
    }

    public boolean hasBirthdays() {
        return !birthdayNames.isEmpty();
    }

    public boolean hasSpecialMessages() {
        return !specialMessages.isEmpty();
    }

    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    public String getBirthdayIntroduction() {
        return "Hôm nay ngày " + getFormattedDate() + " là sinh nhật của "
                + (birthdayNames.size() > 1 ? "các thành viên:" : "thành viên:");
    }

    public String getBirthdayWish() {
        if (birthdayNames.size() > 1) {
            return "Xin gửi đến các anh/chị những lời chúc tốt đẹp nhất! "
                    + "Chúc mọi người luôn mạnh khỏe, tràn đầy niềm vui, hạnh phúc "
                    + "và cùng nhau gặt hái thật nhiều thành công trong công việc cũng như cuộc sống.";
        }
        return "Xin gửi đến bạn những lời chúc tốt đẹp nhất! "
                + "Chúc bạn luôn mạnh khỏe, nhiều niềm vui, hạnh phúc "
                + "và gặt hái thật nhiều thành công trong công việc cũng như cuộc sống.";
    }

    public List<String> getBirthdayNames() {
        return Collections.unmodifiableList(birthdayNames);
    }

    public List<SpecialMessage> getSpecialMessages() {
        return Collections.unmodifiableList(specialMessages);
    }
}
