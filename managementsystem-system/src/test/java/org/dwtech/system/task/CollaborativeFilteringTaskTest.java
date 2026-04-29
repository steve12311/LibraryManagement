package org.dwtech.system.task;

import org.dwtech.system.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CollaborativeFilteringTaskTest {

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private CollaborativeFilteringTask collaborativeFilteringTask;

    @Test
    void shouldDelegateScheduledRebuildToRecommendationService() {
        collaborativeFilteringTask.rebuildSimilarityMatrix();

        verify(recommendationService).rebuildSimilarityMatrix();
    }
}
