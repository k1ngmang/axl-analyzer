package com.kingmang.axl.core;

import com.kingmang.axl.rule.BoolMethodNameRule;
import com.kingmang.axl.rule.ClassLineRule;
import com.kingmang.axl.rule.Rule;

import java.util.Map;
import java.util.Objects;

public final class RuleIdResolver {
    private static final Map<String, Class<? extends Rule>> RULES = Map.of(
            Constant.CLASS_LINE_RULE_ID, ClassLineRule.class,
            Constant.BOOLEAN_NAME_ID, BoolMethodNameRule.class
    );

    public String getId(Class<? extends Rule> ruleClass) {
        Objects.requireNonNull(ruleClass, "ruleClass");

        return RULES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(ruleClass))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No id registered for rule " + ruleClass.getName()
                ));
    }

    public Class<? extends Rule> getRuleClass(String id) {
        Class<? extends Rule> ruleClass = RULES.get(Objects.requireNonNull(id, "id"));
        if (ruleClass == null) {
            throw new IllegalArgumentException("Unknown rule id: " + id);
        }
        return ruleClass;
    }
}
