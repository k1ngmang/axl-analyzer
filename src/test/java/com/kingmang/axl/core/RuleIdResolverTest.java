package com.kingmang.axl.core;

import com.kingmang.axl.rule.ClassLineRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleIdResolverTest {
    private final RuleIdResolver resolver = new RuleIdResolver();

    @Test
    void resolvesIdByRuleClass() {
        assertEquals(Constant.CLASS_LINE_RULE_ID, resolver.getId(ClassLineRule.class));
    }

    @Test
    void resolvesRuleClassById() {
        assertEquals(ClassLineRule.class, resolver.getRuleClass(Constant.CLASS_LINE_RULE_ID));
    }
}
