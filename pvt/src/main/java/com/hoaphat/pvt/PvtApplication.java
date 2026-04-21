package com.hoaphat.pvt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.*;
import java.net.URL;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
public class PvtApplication {
    public static void main(String[] args) throws Exception {
        // Đảm bảo encoding UTF-8 cho toàn bộ hệ thống
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.awt.headless", "false");
        
        SpringApplication.run(PvtApplication.class, args);
        
        if (SystemTray.isSupported()) { 
            showTrayIcon(); 
        }
    }

    private static void showTrayIcon() throws Exception {
        SystemTray tray = SystemTray.getSystemTray();
        URL iconUrl = PvtApplication.class.getResource("/static/img/icon.png");
        Image image = Toolkit.getDefaultToolkit().getImage(iconUrl);

        PopupMenu popup = new PopupMenu();

        // 1. Chỉ giữ lại mục "Tắt server" và sửa lỗi hiển thị
        // Lưu ý: Hãy đảm bảo file .java này được lưu ở định dạng UTF-8
        MenuItem exitItem = new MenuItem("T\u1eaft server");
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(image, "PVT Hòa Phát", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("PVT Server \u0111ang ch\u1ea1y t\u1ea1i localhost");

        // 2. Tính năng click đúp vào icon để mở nhanh trang quản lý
        trayIcon.addActionListener(e -> { 
            try { 
                Desktop.getDesktop().browse(new java.net.URI("http://localhost")); 
            } catch (Exception ex) { 
                ex.printStackTrace(); 
            } 
        });

        tray.add(trayIcon);

        // 3. Thông báo khởi chạy thành công (Thay thế cho MsgBox của VBS)
        trayIcon.displayMessage("PVT Server", "Server \u0111\u00e3 kh\u1edf\u0069 \u0111\u1ed9ng!", TrayIcon.MessageType.INFO);
    }
}