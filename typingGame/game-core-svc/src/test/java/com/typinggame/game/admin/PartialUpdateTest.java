package com.typinggame.game.admin;

import com.typinggame.game.entity.ModelConstant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartialUpdateTest {
    @Test
    void copiesOnlyPresentFields() {
        ModelConstant current = new ModelConstant();
        current.setConstantId(3);
        current.setMaxSpeed(new BigDecimal("150"));
        current.setRefPeakSpeed(new BigDecimal("120"));
        ModelConstant patch = new ModelConstant();
        patch.setMaxSpeed(new BigDecimal("180"));
        PartialUpdate.copyNonNull(patch, current, "constantId");
        assertEquals(new BigDecimal("180"), current.getMaxSpeed());
        assertEquals(new BigDecimal("120"), current.getRefPeakSpeed());
        assertEquals(3, current.getConstantId());
    }
}
