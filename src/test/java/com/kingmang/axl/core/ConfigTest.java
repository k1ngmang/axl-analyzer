package com.kingmang.axl.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @Test
    void rulesAreEnabledByDefault() {
        assertTrue(new Config().isRuleEnabled(Constant.CLASS_LINE_RULE_ID));
    }

    @Test
    void disablesRuleById() {
        Config config = new Config(Map.of(Constant.CLASS_LINE_RULE_ID, false));

        assertFalse(config.isRuleEnabled(Constant.CLASS_LINE_RULE_ID));
    }
}
