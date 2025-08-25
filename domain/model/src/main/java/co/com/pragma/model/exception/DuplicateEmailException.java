package co.com.pragma.model.exception;

public class DuplicateEmailException extends DomainException {
    public DuplicateEmailException(String message) {
        super("EMAIL_ALREADY_EXISTS", message);
    }
}