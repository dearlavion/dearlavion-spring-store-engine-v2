package com.dearlavion.storeengine.catalog;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogRefreshSchedulerTest {

    private static CatalogRefreshScheduler scheduler(TaskScheduler taskScheduler,
                                                     CatalogCacheSettingsRepository repo,
                                                     CatalogCache cache) {
        Mockito.when(repo.findById(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(repo.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        return new CatalogRefreshScheduler(taskScheduler, cache, repo);
    }

    @Test
    void springCronNeedsSixFieldsSoTheUnixFormIsRejected() {
        // The trap this validation exists for: '0 * * * *' is valid Unix cron and invalid here.
        assertThatThrownBy(() -> CatalogRefreshScheduler.validate("0 * * * *"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("six fields");
        assertThatCode(() -> CatalogRefreshScheduler.validate("0 0 * * * *")).doesNotThrowAnyException();
    }

    @Test
    void blankMeansNoScheduleRatherThanInvalid() {
        assertThatCode(() -> CatalogRefreshScheduler.validate("")).doesNotThrowAnyException();
        assertThatCode(() -> CatalogRefreshScheduler.validate(null)).doesNotThrowAnyException();
    }

    @Test
    void reschedulingCancelsThePreviousTimer() {
        // Without the cancel, every save would leave another timer running and refreshes would
        // multiply silently with each edit.
        TaskScheduler taskScheduler = Mockito.mock(TaskScheduler.class);
        ScheduledFuture<?> first = Mockito.mock(ScheduledFuture.class);
        ScheduledFuture<?> second = Mockito.mock(ScheduledFuture.class);
        Mockito.doReturn(first).doReturn(second)
                .when(taskScheduler).schedule(Mockito.any(Runnable.class), Mockito.any(Trigger.class));

        CatalogRefreshScheduler s = scheduler(taskScheduler,
                Mockito.mock(CatalogCacheSettingsRepository.class), Mockito.mock(CatalogCache.class));

        s.reschedule("0 0 * * * *");
        s.reschedule("0 30 * * * *");

        Mockito.verify(first).cancel(false);
        Mockito.verify(taskScheduler, Mockito.times(2))
                .schedule(Mockito.any(Runnable.class), Mockito.any(Trigger.class));
    }

    @Test
    void clearingTheCronCancelsAndSchedulesNothing() {
        TaskScheduler taskScheduler = Mockito.mock(TaskScheduler.class);
        ScheduledFuture<?> future = Mockito.mock(ScheduledFuture.class);
        Mockito.doReturn(future).when(taskScheduler).schedule(Mockito.any(Runnable.class), Mockito.any(Trigger.class));

        CatalogRefreshScheduler s = scheduler(taskScheduler,
                Mockito.mock(CatalogCacheSettingsRepository.class), Mockito.mock(CatalogCache.class));

        s.reschedule("0 0 * * * *");
        s.reschedule("");

        Mockito.verify(future).cancel(false);
        Mockito.verify(taskScheduler, Mockito.times(1))
                .schedule(Mockito.any(Runnable.class), Mockito.any(Trigger.class));
    }

    @Test
    void anInvalidCronIsRejectedBeforeItIsStored() {
        CatalogCacheSettingsRepository repo = Mockito.mock(CatalogCacheSettingsRepository.class);
        CatalogRefreshScheduler s = scheduler(Mockito.mock(TaskScheduler.class), repo, Mockito.mock(CatalogCache.class));

        assertThatThrownBy(() -> s.save("not a cron")).isInstanceOf(IllegalArgumentException.class);

        Mockito.verify(repo, Mockito.never()).save(Mockito.any());
    }

    @Test
    void savingTrimsAndPersists() {
        CatalogCacheSettingsRepository repo = Mockito.mock(CatalogCacheSettingsRepository.class);
        CatalogRefreshScheduler s = scheduler(Mockito.mock(TaskScheduler.class), repo, Mockito.mock(CatalogCache.class));

        assertThat(s.save("  0 0 3 * * *  ").getRefreshCron()).isEqualTo("0 0 3 * * *");
    }
}
