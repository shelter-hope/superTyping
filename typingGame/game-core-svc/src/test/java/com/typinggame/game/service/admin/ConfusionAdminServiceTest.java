package com.typinggame.game.service.admin;

import com.typinggame.entity.user.KeyConfusion;
import com.typinggame.game.repository.KeyConfusionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfusionAdminServiceTest {
    private KeyConfusionRepository repository;
    private ConfusionAdminService service;

    @BeforeEach
    void setUp() {
        repository = mock(KeyConfusionRepository.class);
        service = new ConfusionAdminService(repository);
    }

    private static KeyConfusion row(String expected, String typed, Integer count) {
        KeyConfusion confusion = new KeyConfusion();
        confusion.setExpectedChar(expected);
        confusion.setTypedChar(typed);
        confusion.setConfusionCount(count);
        return confusion;
    }

    @Test
    @SuppressWarnings("unchecked")
    void exposesFlatItemsAndGroupsByExpectedChar() {
        when(repository.countByUserId(7)).thenReturn(3L);
        when(repository.findByUserIdOrderByConfusionCountDesc(eq(7), any())).thenReturn(List.of(
                row("i", "u", 5),
                row("i", "o", 3),
                row("e", "r", 4)
        ));

        Map<String, Object> data = service.getKeyConfusion(7, 20);

        assertEquals(7, data.get("userId"));
        assertEquals(20, data.get("limit"));
        assertEquals(3L, data.get("total"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        assertEquals(3, items.size());
        assertEquals("i", items.get(0).get("expectedChar"));
        assertEquals("u", items.get(0).get("typedChar"));
        assertEquals(5, items.get(0).get("confusionCount"));

        List<Map<String, Object>> groups = (List<Map<String, Object>>) data.get("byExpected");
        assertEquals(2, groups.size());
        assertEquals("i", groups.get(0).get("expectedChar"));
        assertEquals(8, groups.get(0).get("total"));
        assertEquals(2, ((List<?>) groups.get(0).get("items")).size());
        assertEquals("e", groups.get(1).get("expectedChar"));
        assertEquals(4, groups.get(1).get("total"));
    }

    @Test
    void clampsLimitIntoRangeAndCapsAtHundred() {
        when(repository.countByUserId(9)).thenReturn(0L);
        when(repository.findByUserIdOrderByConfusionCountDesc(eq(9), any())).thenReturn(List.of());

        assertEquals(1, service.getKeyConfusion(9, 0).get("limit"));
        assertEquals(100, service.getKeyConfusion(9, 1000).get("limit"));
        assertTrue(((List<?>) service.getKeyConfusion(9, 5).get("items")).isEmpty());
    }

    @Test
    void missingUserReturnsEmptyWithoutQueryingRows() {
        Map<String, Object> data = service.getKeyConfusion(null, 10);

        assertNull(data.get("userId"));
        assertEquals(0L, data.get("total"));
        assertTrue(((List<?>) data.get("items")).isEmpty());
        assertTrue(((List<?>) data.get("byExpected")).isEmpty());
        verify(repository, never()).findByUserIdOrderByConfusionCountDesc(any(), any());
    }

    @Test
    void nullCountIsTreatedAsZero() {
        when(repository.countByUserId(3)).thenReturn(1L);
        when(repository.findByUserIdOrderByConfusionCountDesc(eq(3), any())).thenReturn(List.of(row("a", "s", null)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) service.getKeyConfusion(3, 10).get("items");

        assertEquals(0, items.get(0).get("confusionCount"));
    }
}
