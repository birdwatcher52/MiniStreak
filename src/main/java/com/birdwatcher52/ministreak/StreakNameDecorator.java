package com.birdwatcher52.ministreak;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects: [gold streak #] + our emblem + (optional native chain) into:
 *  - the player's sent chat lines (selected channels)
 *  - the chatbox input line while typing
 *
 * Fix included: players with NO native crown now "learn" an empty native chain
 * immediately, so the input line shows our emblem/number as soon as the emblem
 * is installed (no need to type first).
 */
@Singleton
public final class StreakNameDecorator
{
    private static final Set<ChatMessageType> DECORATED_TYPES = EnumSet.of(
            ChatMessageType.PUBLICCHAT,
            ChatMessageType.FRIENDSCHAT,
            ChatMessageType.CLAN_CHAT,
            ChatMessageType.CLAN_GUEST_CHAT
    );

    private static final Pattern LEADING_IMGS = Pattern.compile("^(?:<img=\\d+>)+");
    private static final Pattern FIRST_IMG    = Pattern.compile("^<img=(\\d+)>");
    private static final String  STREAK_COLOR = "ffdf00"; // gold
    private static final Pattern STREAK_PREFIX_PATTERN =
            Pattern.compile("^<col=" + STREAK_COLOR + ">\\d{1,4}</col>\\s*");

    private final Client client;
    private final ClientThread clientThread;
    private final MiniStreakConfig config;
    private final ModiconInstaller modicons;

    // session state
    private boolean nativeLearned = false;  // we know what native chain (if any) to keep
    private String  nativeChain   = "";     // exact chain to preserve (can be "")
    private int     nativeIconIdx = -1;     // first native icon index, for convenience
    private int     currentStreak = 0;      // provided by plugin each tick
    private boolean active        = false;  // emblem installed + learned native

    @Inject
    public StreakNameDecorator(Client client, ClientThread clientThread,
                               MiniStreakConfig config, ModiconInstaller modicons)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        this.modicons = modicons;
    }

    public void reset()
    {
        nativeLearned = false;
        nativeChain   = "";
        nativeIconIdx = -1;
        currentStreak = 0;
        active        = false;
    }

    public void setCurrentStreak(int v)
    {
        currentStreak = Math.max(0, v);
        // if user hits streak >=1 after learning+install, refresh input
        if (active)
        {
            clientThread.invoke(() -> client.runScript(net.runelite.api.ScriptID.CHAT_PROMPT_INIT));
        }
    }

    // --- Event wiring ---

    @Subscribe
    public void onBeforeRender(BeforeRender ev)
    {
        // If emblem is installed but we haven't "learned" yet, assume empty native chain
        // for main accounts (no crowns). This enables input decoration immediately.
        if (!nativeLearned && modicons.getStreakModIconIdx() >= 0)
        {
            nativeChain = "";      // no native crown to preserve
            nativeLearned = true;
            maybeFlipActive();
        }

        // Try to learn from input if the game has already put icons into it
        if (!nativeLearned)
        {
            tryLearnNativeFromInput();
        }

        // keep input pretty while typing
        updateChatboxInput();
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent ev)
    {
        if ("setChatboxInput".equals(ev.getEventName()))
        {
            updateChatboxInput();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage e)
    {
        // Only decorate SENT lines from the local player, for selected channels.
        if (modicons.getStreakModIconIdx() < 0) return;
        if (client.getLocalPlayer() == null) return;
        if (!DECORATED_TYPES.contains(e.getType())) return;

        if (currentStreak < 1) return; // your rule

        final String me = Text.standardize(client.getLocalPlayer().getName());
        final String sender = Text.standardize(e.getName());
        if (!me.equalsIgnoreCase(sender)) return;

        final String base = (e.getMessageNode().getName() != null)
                ? e.getMessageNode().getName()
                : e.getName();

        final String our = "<img=" + modicons.getStreakModIconIdx() + ">";
        final String prefix = streakPrefix(currentStreak);
        final boolean showNative = config.showNativeIcon();

        // Already in desired form?
        if (base.startsWith(prefix + our)) return;

        // Strip any previously-injected streak prefix
        String cleanBase = base;
        final Matcher prefM = STREAK_PREFIX_PATTERN.matcher(cleanBase);
        if (prefM.find()) cleanBase = prefM.replaceFirst("");

        // Split leading chain
        String leading = "";
        String rest = cleanBase;
        final Matcher chain = LEADING_IMGS.matcher(cleanBase);
        if (chain.find())
        {
            leading = chain.group(0);
            rest    = cleanBase.substring(chain.end());

            // Learn native idx (legacy convenience)
            if (nativeIconIdx < 0)
            {
                final Matcher first = FIRST_IMG.matcher(leading);
                if (first.find())
                {
                    try
                    {
                        final int idx = Integer.parseInt(first.group(1));
                        if (idx != modicons.getStreakModIconIdx())
                        {
                            nativeIconIdx = idx;
                        }
                    }
                    catch (NumberFormatException ignored) {}
                }
            }

            // Strip our emblem if it snuck into leading
            leading = leading.replaceFirst("<img=" + modicons.getStreakModIconIdx() + ">", "");

            // Respect toggle: optionally remove one native tag from leading
            if (!showNative && nativeIconIdx >= 0)
            {
                leading = leading.replaceFirst("<img=" + nativeIconIdx + ">", "");
            }
        }

        // Compose
        final boolean leadingHasNative = nativeIconIdx >= 0 && leading.contains("<img=" + nativeIconIdx + ">");
        final String nativeTag = (nativeIconIdx >= 0 && showNative) ? "<img=" + nativeIconIdx + ">" : "";

        final String newName = prefix
                + our
                + ((showNative && !leadingHasNative) ? nativeTag : "")
                + leading
                + rest;

        e.getMessageNode().setName(newName);
// Nudge the chatbox to redraw on older/newer RL versions:
        client.refreshChat();

    }

    // --- Internals ---

    private void tryLearnNativeFromInput()
    {
        final Widget w = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        if (w == null || w.isHidden()) return;
        if (client.getLocalPlayer() == null) return;

        final String text = w.getText();
        if (text == null || text.isEmpty()) return;

        final int colon = text.indexOf(':');
        if (colon < 0) return;

        final String prefix = text.substring(0, colon); // "<img=...><img=...>Name"
        final Matcher m = LEADING_IMGS.matcher(prefix);

        if (m.find())
        {
            String chain = m.group(0);
            if (modicons.getStreakModIconIdx() >= 0)
            {
                chain = chain.replaceFirst("<img=" + modicons.getStreakModIconIdx() + ">", "");
            }
            nativeChain = chain; // may be empty after strip — that's fine
            nativeLearned = true;
            maybeFlipActive();
            return;
        }

        // NEW: No native icons present → treat as learned empty chain.
        nativeChain = "";
        nativeLearned = true;
        maybeFlipActive();
    }

    private void updateChatboxInput()
    {
        if (!active) return;
        if (modicons.getStreakModIconIdx() < 0) return;
        if (currentStreak < 1) return;

        final Widget w = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        if (w == null || w.isHidden()) return;

        if (client.getLocalPlayer() == null) return;
        final String rsn = client.getLocalPlayer().getName();
        if (rsn == null) return;

        final String text = w.getText();
        final String[] parts = text.split(":", 2);
        if (parts.length < 2) return; // nothing typed yet

        final String our = "<img=" + modicons.getStreakModIconIdx() + ">";
        final String prefix = streakPrefix(currentStreak);
        final String maybeNative = config.showNativeIcon() ? nativeChain : "";

        // Canonical order: [#] + OUR + (nativeChain?) + RSN + ":" + typed
        w.setText(prefix + our + maybeNative + Text.removeTags(rsn) + ":" + parts[1]);
    }

    private void maybeFlipActive()
    {
        if (!active && nativeLearned && modicons.getStreakModIconIdx() >= 0)
        {
            active = true;
            client.runScript(net.runelite.api.ScriptID.CHAT_PROMPT_INIT);
        }
    }

    private static String streakPrefix(int streak)
    {
        if (streak < 1) return "";
        return "<col=" + STREAK_COLOR + ">" + streak + "</col> ";
    }
}
