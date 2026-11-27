package com.example.betting.messaging;

import com.example.betting.exception.MessageValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class InboundMessageValidator {

    private final Validator validator;

    public InboundMessageValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(T message, String messageType) {
        if (message == null) {
            throw new MessageValidationException(messageType + " message is required");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(message);
        if (violations.isEmpty()) {
            return message;
        }

        String details = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        throw new MessageValidationException(messageType + " message is invalid: " + details);
    }
}
