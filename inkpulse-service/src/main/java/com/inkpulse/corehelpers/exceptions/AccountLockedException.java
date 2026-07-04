package com.inkpulse.corehelpers.exceptions;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AccountLockedException extends BaseException {
    private final LocalDateTime unlockAt;

    public AccountLockedException(String message, LocalDateTime unlockAt) {
        super(message, 423, "ACCOUNT_LOCKED");
        this.unlockAt = unlockAt;
    }
}
