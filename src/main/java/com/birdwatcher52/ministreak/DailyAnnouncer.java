package com.birdwatcher52.ministreak;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.ZoneOffset;

import net.runelite.client.Notifier;

/** Once-per-UTC-day reminder via RuneLite Notifier (no toast, no sound here). */
@Singleton
final class DailyAnnouncer
{
    private final Notifier notifier;

    @Inject
    DailyAnnouncer(Notifier notifier)
    {
        this.notifier = notifier;
    }

    void maybeNotifyOnReset(StreakState s)
    {
        maybeNotifyOnce(s);
    }

    void maybeNotifyOnLogin(StreakState s)
    {
        maybeNotifyOnce(s);
    }

    private void maybeNotifyOnce(StreakState s)
    {
        final String today = LocalDate.now(ZoneOffset.UTC).toString();

        // De-dupe: only once per UTC day
        if (today.equals(s.getLastAnnouncementDateUTC()))
        {
            return;
        }

        // Suppress if already fully completed today
        if (s.bothDoneTodayUTC())
        {
            // Stamp anyway so we don't show again on subsequent logins this UTC day
            s.setLastAnnouncementDateUTC(today);
            return;
        }

        final String line = (s.getCurrentStreak() <= 0)
                ? "Complete today's mini tasks to start a streak."
                : "Complete today's mini tasks to keep up your streak.";

        try
        {
            notifier.notify(line);
        }
        catch (Exception ignored)
        {
            // Keep UX resilient even if Notifier is disabled or throws
        }

        s.setLastAnnouncementDateUTC(today);
    }
}
