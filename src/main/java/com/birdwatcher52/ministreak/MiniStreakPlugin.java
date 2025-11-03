package com.birdwatcher52.ministreak;

import com.birdwatcher52.ministreak.util.TinyWav;
import com.google.inject.Provides;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Locale;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;

import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@PluginDescriptor(
        name = "MiniStreak",
        description = "Help stay consistent with planting herbs and setting up birdhouses",
        tags = {"streak", "daily", "birdhouse", "herb", "ministreak"}
)
public class MiniStreakPlugin extends Plugin
{
    @Inject private MiniStreakConfig config;
    @Inject private ConfigManager configManager;
    @Inject private MiniStreakStorage storage;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ToastService toast;
    @Inject private Notifier notifier;

    @Inject private DailyAnnouncer dailyAnnouncer;

    @Inject private Client client;
    @Inject private ClientThread clientThread;

    // NEW: extracted services
    @Inject private EventBus eventBus;
    @Inject private ModiconInstaller modicons;
    @Inject private StreakNameDecorator nameDecorator;

    private final ResetService resetService = new ResetService();
    private final DailyCompletionService completeService = new DailyCompletionService();

    private StreakState state;
    private MiniStreakPanel panel;
    private NavigationButton navButton;

    private boolean birdhouseMarkedThisTick = false;
    private boolean herbMarkedThisTick = false;

    private final TinyWav wav = new TinyWav();

    @Override
    protected void startUp() throws Exception
    {
        // Register our decorator listeners
        eventBus.register(nameDecorator);

        // Reset helpers
        modicons.reset();
        nameDecorator.reset();

        // Register sounds
        wav.registerResource("chime", "/com/birdwatcher52/ministreak/chime.wav");
        wav.registerResource("mini_chime", "/com/birdwatcher52/ministreak/mini_chime.wav");

        state = new StreakState();
        storage.loadInto(state);

        panel = new MiniStreakPanel();
        panel.setDebugVisible(config.debugMode());
        panel.wireDebug(
                this::handleBirdhouseMark,
                this::handleHerbMark,
                () -> { forceRollOnce(); storage.save(state); panel.refresh(state); }
        );

        if (config.showSidebar())
        {
            ensureNavButton();
            clientToolbar.addNavigation(navButton);
        }

        // Kick emblem install; nameDecorator will activate once native learned + emblem ready
        modicons.ensureInstalled(config.streakEmblem());

        // Feed current streak into the decorator
        nameDecorator.setCurrentStreak(state.getCurrentStreak());

        panel.refresh(state);
    }

    @Override
    protected void shutDown()
    {
        storage.save(state);

        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        panel = null;

        // Unregister listeners
        eventBus.unregister(nameDecorator);

        modicons.reset();
        nameDecorator.reset();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        if (e.getGameState() == GameState.LOGGED_IN)
        {
            modicons.ensureInstalled(config.streakEmblem());
            dailyAnnouncer.maybeNotifyOnLogin(state);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        // emblem retry loop (short-lived)
        modicons.tickRetry();

        // Detect UTC day roll
        final String seenBefore = state.getLastSeenDateUTC();
        resetService.handleDailyRoll(state);
        final String seenAfter = state.getLastSeenDateUTC();
        if (!seenAfter.equals(seenBefore))
        {
            dailyAnnouncer.maybeNotifyOnReset(state);
        }

        birdhouseMarkedThisTick = false;
        herbMarkedThisTick = false;

        toast.processQueue();

        storage.save(state);
        if (panel != null) panel.refresh(state);

        // keep decorator in sync with streak
        nameDecorator.setCurrentStreak(state.getCurrentStreak());
    }

    // ---- Daily detection (unchanged) ----
    @Subscribe
    public void onChatMessage(ChatMessage e)
    {
        final ChatMessageType t = e.getType();

        if (t == ChatMessageType.GAMEMESSAGE || t == ChatMessageType.SPAM)
        {
            final String msg = Text.removeTags(e.getMessage()).toLowerCase(Locale.ROOT);

            if (!birdhouseMarkedThisTick
                    && (msg.contains("birdhouse") || msg.contains("bird house"))
                    && msg.contains("trap is now full")
                    && (msg.contains("will start to catch birds") || msg.contains("will begin to catch birds")))
            {
                birdhouseMarkedThisTick = true;
                handleBirdhouseMark();
                return;
            }

            if (!herbMarkedThisTick
                    && msg.startsWith("you plant")
                    && msg.contains("seed in the herb patch"))
            {
                herbMarkedThisTick = true;
                handleHerbMark();
            }
        }
    }

    private void handleBirdhouseMark()
    {
        final boolean noneDoneYet = !state.birdhouseDoneTodayUTC() && !state.herbDoneTodayUTC();
        final String prevDaily = state.getLastCompletionDateUTC();
        completeService.onBirdhouseMarked(state);

        if (noneDoneYet) notifyHalfway();

        if (!prevDaily.equals(state.getLastCompletionDateUTC()))
        {
            int streak = state.getCurrentStreak();
            String dayWord = (streak == 1) ? "day" : "days";
            toast.enqueue("Daily Mini Complete!", "Your current streak is " + streak + " " + dayWord + "!", Color.WHITE);
            wav.play("chime", 100);
        }

        storage.save(state);
        if (panel != null) panel.refresh(state);

        nameDecorator.setCurrentStreak(state.getCurrentStreak());
    }

    private void handleHerbMark()
    {
        final boolean noneDoneYet = !state.birdhouseDoneTodayUTC() && !state.herbDoneTodayUTC();
        final String prevDaily = state.getLastCompletionDateUTC();
        completeService.onHerbMarked(state);

        if (noneDoneYet) notifyHalfway();

        if (!prevDaily.equals(state.getLastCompletionDateUTC()))
        {
            int streak = state.getCurrentStreak();
            String dayWord = (streak == 1) ? "day" : "days";
            toast.enqueue("Daily Mini Complete!", "Your current streak is " + streak + " " + dayWord + "!", Color.WHITE);
            wav.play("chime", 100);
        }

        storage.save(state);
        if (panel != null) panel.refresh(state);

        nameDecorator.setCurrentStreak(state.getCurrentStreak());
    }

    private void notifyHalfway()
    {
        try { notifier.notify("Halfway done (1/2)!"); } catch (Exception ignored) {}
        try { wav.play("mini_chime", 85); } catch (Exception ignored) {}
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"ministreak".equals(e.getGroup())) return;

        if (panel != null) panel.setDebugVisible(config.debugMode());

        if (config.showSidebar())
        {
            ensureNavButton();
            clientToolbar.addNavigation(navButton);
        }
        else if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }

        // Emblem selection or native toggle changes:
        modicons.ensureInstalled(config.streakEmblem());

        // Refresh input immediately to reflect toggle changes
        clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));

        // Streak display depends on toggle/state; decorator reads config live.
        nameDecorator.setCurrentStreak(state.getCurrentStreak());
    }

    @Provides
    MiniStreakConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(MiniStreakConfig.class);
    }

    private void ensureNavButton()
    {
        if (navButton != null) return;

        final BufferedImage icon =
                ImageUtil.loadImageResource(getClass(), "/com/birdwatcher52/ministreak/icon.png");

        navButton = NavigationButton.builder()
                .tooltip("MiniStreak")
                .priority(-9999)
                .icon(icon)
                .panel(panel)
                .build();
    }

    private void forceRollOnce()
    {
        state.setCurrentStreak(0);
        state.setLastSeenDateUTC("1900-01-01");
        state.setLastBirdhouseDateUTC("");
        state.setLastHerbDateUTC("");
        state.setLastCompletionDateUTC("");
        state.setLastAnnouncementDateUTC("");

        resetService.handleDailyRoll(state);
        storage.save(state);
        if (panel != null) panel.refresh(state);
    }
}
