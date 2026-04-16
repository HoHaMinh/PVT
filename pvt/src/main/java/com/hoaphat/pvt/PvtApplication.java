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
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(PvtApplication.class, args);
        if (SystemTray.isSupported()) { showTrayIcon(); }
    }
    private static void showTrayIcon() throws Exception {
        SystemTray tray = SystemTray.getSystemTray();
        URL iconUrl = PvtApplication.class.getResource("/static/img/icon.png");
        Image image = Toolkit.getDefaultToolkit().getImage(iconUrl);
        PopupMenu popup = new PopupMenu();
        MenuItem openItem = new MenuItem("Mở trình duyệt");
        openItem.addActionListener(e -> { try { Desktop.getDesktop().browse(new java.net.URI("http://localhost")); } catch (Exception ex) { ex.printStackTrace(); } });
        MenuItem exitItem = new MenuItem("Tắt server");
        exitItem.addActionListener(e -> System.exit(0));
        popup.add(openItem); popup.addSeparator(); popup.add(exitItem);
        TrayIcon trayIcon = new TrayIcon(image, "PVT Hòa Phát", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("PVT Server đang chạy tại localhost");
        trayIcon.addActionListener(e -> { try { Desktop.getDesktop().browse(new java.net.URI("http://localhost")); } catch (Exception ex) { ex.printStackTrace(); } });
        tray.add(trayIcon);
        trayIcon.displayMessage("PVT Server", "Server đã khởi động! Truy cập: http://localhost", TrayIcon.MessageType.INFO);
    }
}