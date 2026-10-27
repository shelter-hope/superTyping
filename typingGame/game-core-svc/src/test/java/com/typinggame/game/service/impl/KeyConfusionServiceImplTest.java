package com.typinggame.game.service.impl;

import com.typinggame.dto.stats.KeyConfusionUpdateDTO;
import com.typinggame.entity.user.KeyConfusion;
import com.typinggame.game.repository.KeyConfusionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeyConfusionServiceImplTest {

    private final KeyConfusionRepository repository = mock(KeyConfusionRepository.class);
    private final KeyConfusionServiceImpl service = new KeyConfusionServiceImpl(repository);

    @Test
    void aggregatesDuplicatePairsIntoOneRow() {
        Map<String, KeyConfusion> store = new HashMap<>();
        when(repository.findByUserIdAndExpectedCharAndTypedChar(any(), anyString(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        store.get(invocation.getArgument(1) + ">" + invocation.getArgument(2))));
        when(repository.save(any())).thenAnswer(invocation -> {
            KeyConfusion row = invocation.getArgument(0);
            store.put(row.getExpectedChar() + ">" + row.getTypedChar(), row);
            return row;
        });

        service.batchUpdate(7, List.of(update("i", "t", 1), update("i", "t", 2)));

        assertEquals(1, store.size());
        KeyConfusion saved = store.get("i>t");
        assertEquals(7, saved.getUserId());
        assertEquals(3, saved.getConfusionCount());
    }

    @Test
    void incrementsExistingRow() {
        KeyConfusion existing = new KeyConfusion();
        existing.setExpectedChar("a");
        existing.setTypedChar("s");
        existing.setConfusionCount(4);
        when(repository.findByUserIdAndExpectedCharAndTypedChar(7, "a", "s")).thenReturn(Optional.of(existing));

        service.batchUpdate(7, List.of(update("a", "s", 2)));

        assertEquals(6, existing.getConfusionCount());
    }

    @Test
    void skipsSameInvalidAndNonPositiveEntries() {
        service.batchUpdate(7, List.of(
                update("a", "a", 1),
                update("ab", "s", 1),
                update(" ", "s", 1),
                update("a", "s", 0),
                update(null, "s", 1)));

        verify(repository, never()).save(any());
    }

    @Test
    void clampsSingleEntryCount() {
        when(repository.findByUserIdAndExpectedCharAndTypedChar(7, "q", "p")).thenReturn(Optional.empty());
        service.batchUpdate(7, List.of(update("q", "p", 50_000)));

        ArgumentCaptor<KeyConfusion> captor = ArgumentCaptor.forClass(KeyConfusion.class);
        verify(repository).save(captor.capture());
        assertTrue(captor.getValue().getConfusionCount() <= 1000);
    }

    @Test
    void confusionMapKeepsCountOrderAndLimits() {
        when(repository.findByUserIdOrderByConfusionCountDesc(anyInt(), any(PageRequest.class)))
                .thenReturn(List.of(row("i", "t", 9), row("i", "r", 5), row("i", "e", 2), row("a", "s", 7)));

        Map<String, List<String>> map = service.getConfusionMap(7, 2, 2);

        assertEquals(List.of("i", "a"), List.copyOf(map.keySet()));
        assertEquals(List.of("t", "r"), map.get("i"));
        assertEquals(List.of("s"), map.get("a"));
    }

    @Test
    void emptyReportDoesNothing() {
        service.batchUpdate(7, List.of());
        service.batchUpdate(null, List.of(update("a", "s", 1)));

        verify(repository, never()).save(any());
        verify(repository, never()).findByUserIdAndExpectedCharAndTypedChar(any(), anyString(), anyString());
    }

    @Test
    void countIsZeroForAnonymousUser() {
        assertEquals(0L, service.countByUserId(null));
        assertTrue(service.getConfusionMap(null, 5, 3).isEmpty());
    }

    private static KeyConfusionUpdateDTO update(String expected, String typed, int count) {
        KeyConfusionUpdateDTO dto = new KeyConfusionUpdateDTO();
        dto.setExpectedChar(expected);
        dto.setTypedChar(typed);
        dto.setCount(count);
        return dto;
    }

    private static KeyConfusion row(String expected, String typed, int count) {
        KeyConfusion row = new KeyConfusion();
        row.setExpectedChar(expected);
        row.setTypedChar(typed);
        row.setConfusionCount(count);
        return row;
    }
}
