package com.inkpulse.pipeline;

import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class EligibilityContext<TEntity> {
    private final TEntity entity;
    private boolean rejected;
    private String rejectionReason;
    private final List<String> warnings = new ArrayList<>();

    public EligibilityContext(TEntity entity) {
        this.entity = entity;
    }

    public void reject(String reason) {
        this.rejected = true;
        this.rejectionReason = reason;
    }

    public void warn(String reason) {
        this.warnings.add(reason);
    }
}
