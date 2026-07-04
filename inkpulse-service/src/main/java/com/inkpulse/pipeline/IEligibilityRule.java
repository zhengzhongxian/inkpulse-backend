package com.inkpulse.pipeline;

public interface IEligibilityRule<TEntity> {
    int getOrder();
    void evaluate(EligibilityContext<TEntity> context);
}
