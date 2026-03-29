package kkkvd.docflow.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import kkkvd.docflow.dto.NotificationResponse;
import kkkvd.docflow.entities.Notification;
import kkkvd.docflow.entities.User;
import kkkvd.docflow.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Сервис уведомлений.
// Отвечает за два канала уведомлений:
//   1. Внутренние — хранятся в БД, отображаются в колокольчике в шапке
//   2. Email — отправляются через SMTP (настройки в application.properties)
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender javaMailSender;
    private final MailSender mailSender;

    // Создать внутреннее уведомление для пользователя.
    // Вызывается из DocumentService при каждом событии документа.
    @Transactional
    public void send(User recipient, String title, String message,
                     Notification.NotificationType type, String link) {

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    // Получить все уведомления пользователя.
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll(User user) {
        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user).stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    // Количество непрочитанных — для бейджа на колокольчике.
    @Transactional(readOnly = true)
    public long countUnread(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    // Пометить уведомление как прочитанное.
    @Transactional
    public void markAsRead(Long notificationId, User currentUser) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getId().equals(currentUser.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    // Пометить все уведомления пользователя как прочитанные.
    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // Email-уведомления
    // @Async — письмо отправляется в фоне, не блокирует основной поток.
    // Если SMTP не настроен — просто логируем ошибку, система не падает.
    @Async
    public void sendEmail(String toEmail, String subject, String htmlBody) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
         try {
             MimeMessage message = javaMailSender.createMimeMessage();
             MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
             helper.setTo(toEmail);
             helper.setSubject(subject);
             helper.setText(htmlBody, true);
             javaMailSender.send(message);
         } catch (MessagingException e) {
             log.warn("Не удалось отправить email на {}: {}",  toEmail, e.getMessage());
         }
    }
}
