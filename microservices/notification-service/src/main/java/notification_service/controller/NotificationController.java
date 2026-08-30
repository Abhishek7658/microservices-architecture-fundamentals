package notification_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    @GetMapping("/notifications")
    public String getNotifications() {
        return "Notification Service is working";
    }
}