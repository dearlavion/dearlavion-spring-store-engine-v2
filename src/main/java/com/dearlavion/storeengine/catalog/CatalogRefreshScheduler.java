package com.dearlavion.storeengine.catalog;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

/**
 * Runs the catalog refresh on an admin-editable cron.
 *
 * <p>{@code @Scheduled} can't be used here: its cron is fixed at compile time, and this one is
 * changed from the settings page. So the task is scheduled manually and re-scheduled whenever the
 * expression changes — cancelling the previous future first, or every save would leave another
 * timer running and the refreshes would multiply silently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogRefreshScheduler {

    private final TaskScheduler taskScheduler;
    private final CatalogCache catalog;
    private final CatalogCacheSettingsRepository settings;

    private ScheduledFuture<?> scheduled;

    /** Validates without scheduling, so a bad expression is rejected at save time. */
    public static void validate(String cron) {
        if (cron == null || cron.isBlank()) return; // blank means "no schedule"
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException(
                    "Not a valid cron expression: '" + cron + "'. Spring cron takes six fields "
                            + "starting with seconds — hourly is '0 0 * * * *', not '0 * * * *'.");
        }
    }

    public CatalogCacheSettings current() {
        return settings.findById(CatalogCacheSettings.SINGLETON_ID).orElseGet(CatalogCacheSettings::new);
    }

    @PostConstruct
    void applyStoredScheduleOnStartup() {
        reschedule(current().getRefreshCron());
    }

    public CatalogCacheSettings save(String cron) {
        validate(cron);
        CatalogCacheSettings stored = current();
        stored.setId(CatalogCacheSettings.SINGLETON_ID);
        stored.setRefreshCron(cron == null ? "" : cron.trim());
        stored.setUpdatedAt(Instant.now());
        CatalogCacheSettings saved = settings.save(stored);
        reschedule(saved.getRefreshCron());
        return saved;
    }

    /**
     * Synchronized because two admins saving at once could otherwise both cancel the same future and
     * both schedule a new one, leaving a timer nobody holds a reference to and can never cancel.
     */
    public synchronized void reschedule(String cron) {
        if (scheduled != null) {
            scheduled.cancel(false); // let a refresh already in flight finish
            scheduled = null;
        }
        if (cron == null || cron.isBlank()) {
            log.warn("No catalog refresh schedule — the snapshot will only update when an admin "
                    + "presses reset or the service restarts.");
            return;
        }
        scheduled = taskScheduler.schedule(this::runScheduledRefresh, new CronTrigger(cron));
        log.info("Catalog refresh scheduled with cron '{}'.", cron);
    }

    /**
     * Never lets an exception escape into the scheduler: an uncaught one cancels the repeating task
     * for the lifetime of the process, so a single Atlas blip would silently end all future
     * refreshes. CatalogCache.refresh() already keeps the old snapshot on failure; this stops the
     * schedule itself from dying with it.
     */
    private void runScheduledRefresh() {
        try {
            catalog.refresh("scheduled: " + current().getRefreshCron());
        } catch (RuntimeException e) {
            log.error("Scheduled catalog refresh failed; the schedule remains active.", e);
        }
    }
}
