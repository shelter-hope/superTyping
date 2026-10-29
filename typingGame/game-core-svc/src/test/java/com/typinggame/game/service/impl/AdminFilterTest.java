package com.typinggame.game.service.impl;

import com.typinggame.common.admin.AdminFilter;
import com.typinggame.game.entity.GameRecord;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminFilterTest {

    @Test
    void ignoresNullAndBlankValues() {
        AdminFilter<GameRecord> f = AdminFilter.of(GameRecord.class)
                .eq("userId", null)
                .eq("gameMode", "  ")
                .like("gameMode", "")
                .between("createTime", null, (Date) null);

        assertEquals(0, f.size());
    }

    @Test
    void addsOneConditionPerPresentValue() {
        AdminFilter<GameRecord> f = AdminFilter.of(GameRecord.class)
                .eq("userId", 3)
                .like("gameMode", "classic")
                .between("createTime", new Date(0), new Date());

        assertEquals(4, f.size());
    }
}
