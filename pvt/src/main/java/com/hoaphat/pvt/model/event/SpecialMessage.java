package com.hoaphat.pvt.model.event;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class SpecialMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String message;
    private LocalDateTime birthday;

    public SpecialMessage() {
    }

    public SpecialMessage(Integer id, String message, LocalDateTime birthday) {
        this.id = id;
        this.message = message;
        this.birthday = birthday;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDateTime birthday) {
        this.birthday = birthday;
    }

    public String getFormattedDate() {
        return birthday == null ? "" : birthday.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    public String getDisplayTitle() {
        if (birthday == null) {
            return "Thông điệp đặc biệt";
        }
        int day = birthday.getDayOfMonth();
        int month = birthday.getMonthValue();
        if (day == 8 && month == 3) {
            return "Mừng Ngày Quốc tế Phụ nữ 08/03";
        }
        if (day == 20 && month == 10) {
            return "Mừng Ngày Phụ nữ Việt Nam 20/10";
        }
        return "Thông điệp đặc biệt ngày " + getFormattedDate();
    }
}
