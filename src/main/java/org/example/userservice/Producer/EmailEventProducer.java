package org.example.userservice.Producer;
import org.example.userservice.dtos.SendEmailEventDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmailEventProducer {
    private final KafkaTemplate<String, SendEmailEventDto> kafkaTemplate;

    public EmailEventProducer(KafkaTemplate<String, SendEmailEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEmailEvent(SendEmailEventDto emailDto) {
        kafkaTemplate.send("sendEmail", emailDto);
    }
}