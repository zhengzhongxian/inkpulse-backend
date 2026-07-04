package com.inkpulse.pipeline;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class EligibilityPipeline<TEntity> {
    private final List<IEligibilityRule<TEntity>> rules;

    public EligibilityPipeline(Collection<IEligibilityRule<TEntity>> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(IEligibilityRule::getOrder))
                .toList();
    }

    public EligibilityContext<TEntity> run(TEntity entity) {
        EligibilityContext<TEntity> context = new EligibilityContext<>(entity);

        for (IEligibilityRule<TEntity> rule : rules) {
            rule.evaluate(context);
            if (context.isRejected()) {
                return context;
            }
        }

        return context;
    }
}
