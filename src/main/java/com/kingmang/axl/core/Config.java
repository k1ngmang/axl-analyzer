package com.kingmang.axl.core;

import java.util.Map;
import java.util.Objects;

public final class Config {
    private final Map<String, Boolean> enabledRules;

    public Config() {
        this(Map.of());
    }

    public Config(Map<String, Boolean> enabledRules) {
        this.enabledRules = Map.copyOf(Objects.requireNonNull(enabledRules, "enabledRules"));
    }

    public boolean isRuleEnabled(String ruleId) {
        return enabledRules.getOrDefault(Objects.requireNonNull(ruleId, "ruleId"), true);
    }
}
