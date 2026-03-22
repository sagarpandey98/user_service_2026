package org.example.userservice.Producer;

import org.example.userservice.dtos.SendEmailEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
public class EmailEventProducer {

    private static final Logger log =
        LoggerFactory.getLogger(EmailEventProducer.class);

    @Value("${email.service.url:http://localhost:8080}")
    private String emailServiceUrl;

    private final RestTemplate restTemplate;

    public EmailEventProducer() {
        this.restTemplate = new RestTemplate();
    }

    public void sendEmailEvent(SendEmailEventDto emailDto) {
        try {
            String url = emailServiceUrl + "/api/emails/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SendEmailEventDto> request =
                new HttpEntity<>(emailDto, headers);

            String response = restTemplate.postForObject(
                url, request, String.class
            );

            log.info("Email sent successfully to {}: {}",
                emailDto.getTo(), response);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}",
                emailDto.getTo(), e.getMessage(), e);
            // Non-blocking — don't throw, just log
            // OTP is still saved in DB even if email fails
        }
    }
}