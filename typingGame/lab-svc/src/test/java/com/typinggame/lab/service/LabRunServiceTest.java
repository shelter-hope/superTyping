package com.typinggame.lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typinggame.lab.dto.LabRunRequest;
import com.typinggame.lab.dto.LabRunView;
import com.typinggame.lab.entity.LabAlgorithm;
import com.typinggame.lab.entity.LabRun;
import com.typinggame.lab.repository.LabAlgorithmRepository;
import com.typinggame.lab.repository.LabRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LabRunServiceTest {

    private LabRunRepository runs;
    private LabAlgorithmRepository algorithms;
    private LabCatalogService catalog;
    private LabRunService service;

    @BeforeEach
    void setUp() {
        runs = mock(LabRunRepository.class);
        algorithms = mock(LabAlgorithmRepository.class);
        catalog = mock(LabCatalogService.class);
        service = new LabRunService(runs, algorithms, catalog, new ObjectMapper());
        LabAlgorithm algorithm = new LabAlgorithm();
        algorithm.setId("pso");
        algorithm.setSchemaVersion(1);
        algorithm.setEnabled(true);
        when(algorithms.findById("pso")).thenReturn(Optional.of(algorithm));
        when(runs.save(any())).thenAnswer(invocation -> {
            LabRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(9L);
            }
            return run;
        });
    }

    @Test
    void createsCompletedSnapshot() {
        LabRunRequest request = new LabRunRequest(
                "pso", "pso", 1, "试跑", null, 7,
                Map.of("count", 80, "inertia", 0.7, "cognitive", 1.4, "social", 1.4, "maxSpeed", 3),
                null, "completed", null, null, 1200,
                Map.of("bestFitness", 1.2),
                Map.of("t", List.of(0, 250), "series", Map.of("bestFitness", List.of(1.5, 1.2)), "maxPoints", 240, "policy", "stride"),
                Map.of("width", 800, "height", 480)
        );

        LabRunView view = service.create(3, request);

        assertEquals(9L, view.id());
        assertEquals("pso", view.algorithmId());
        assertEquals("client_snapshot", view.engineKind());
        assertEquals(7, view.seed());
        assertEquals(1.2, ((Number) view.summary().get("bestFitness")).doubleValue());
        assertTrue(view.metrics().containsKey("series"));
    }

    @Test
    void rejectsUnknownAlgorithm() {
        when(algorithms.findById("nope")).thenReturn(Optional.empty());
        LabRunRequest request = new LabRunRequest(
                "nope", null, 1, null, null, 1, Map.of("count", 1), null, null, null, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.create(3, request));
    }

    @Test
    void listOmitsMetrics() {
        LabRun run = new LabRun();
        run.setId(1L);
        run.setUserId(3);
        run.setAlgorithmId("pso");
        run.setPluginId("pso");
        run.setSchemaVersion(1);
        run.setEngineKind("client_snapshot");
        run.setSeed(1);
        run.setParamsJson("{\"count\":80,\"inertia\":0.7,\"cognitive\":1.4,\"social\":1.4,\"maxSpeed\":3}");
        run.setStatus("completed");
        run.setMetricsJson("{\"t\":[0],\"series\":{\"bestFitness\":[1]}}");
        run.setCreatedAt(LocalDateTime.now());
        when(runs.findByUserIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(run)));

        Page<LabRunView> page = service.list(3, null, 0, 20);

        assertNull(page.getContent().get(0).metrics());
        assertEquals("pso", page.getContent().get(0).algorithmId());
    }

    @Test
    void deleteOwnRun() {
        LabRun run = new LabRun();
        run.setId(4L);
        run.setUserId(3);
        when(runs.findByIdAndUserId(4L, 3)).thenReturn(Optional.of(run));

        service.delete(3, 4L);

        verify(runs).delete(run);
    }

    @Test
    void adminSearchFiltersWithoutMetrics() {
        LabRun run = new LabRun();
        run.setId(2L);
        run.setUserId(8);
        run.setAlgorithmId("pso");
        run.setPluginId("pso");
        run.setSchemaVersion(1);
        run.setEngineKind("client_snapshot");
        run.setSeed(3);
        run.setParamsJson("{\"count\":80,\"inertia\":0.7,\"cognitive\":1.4,\"social\":1.4,\"maxSpeed\":3}");
        run.setStatus("completed");
        run.setMetricsJson("{\"t\":[0],\"series\":{\"bestFitness\":[1]}}");
        run.setCreatedAt(LocalDateTime.now());
        when(runs.adminSearch(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(run)));

        Page<LabRunView> page = service.adminSearch(8, "pso", null, null, 0, 20);

        assertEquals(8, page.getContent().get(0).userId());
        assertNull(page.getContent().get(0).metrics());
    }
}
