package com.birdwatcher52.ministreak;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.api.ScriptID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Appends our emblem as a new modicon and exposes its index.
 * Handles early-client race by retrying for a few ticks.
 */
@Singleton
public final class ModiconInstaller
{
    private static final int MAX_RETRY_TICKS = 120;

    private final Client client;
    private final ClientThread clientThread;
    private final SpriteManager spriteManager;

    @Getter
    private int streakModIconIdx = -1;

    private StreakEmblems loaded;
    private int retryTicks = 0;
    private boolean installed = false;

    @Inject
    public ModiconInstaller(Client client, ClientThread clientThread, SpriteManager spriteManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.spriteManager = spriteManager;
    }

    public void reset()
    {
        streakModIconIdx = -1;
        loaded = null;
        retryTicks = 0;
        installed = false;
    }

    public void tickRetry()
    {
        if (!installed && retryTicks++ < MAX_RETRY_TICKS && loaded != null)
        {
            ensureInstalled(loaded);
        }
    }

    public void ensureInstalled(StreakEmblems emblem)
    {
        if (emblem == null) return;

        // Already installed and unchanged
        if (installed && loaded == emblem && streakModIconIdx >= 0)
        {
            return;
        }

        loaded = emblem;

        spriteManager.getSpriteAsync(emblem.getSpriteId(), 0, sprite -> {
            if (sprite == null) return;

            clientThread.invoke(() -> {
                try
                {
                    BufferedImage img = sprite;
                    if (img.getWidth() != 13 || img.getHeight() != 13)
                    {
                        img = ImageUtil.resizeImage(img, 13, 13);
                    }

                    final IndexedSprite is = ImageUtil.getImageIndexedSprite(img, client);
                    final IndexedSprite[] mods = client.getModIcons();
                    if (mods == null)
                    {
                        // Not ready yet; on next tick we'll retry.
                        installed = false;
                        return;
                    }

                    final IndexedSprite[] newMods = Arrays.copyOf(mods, mods.length + 1);
                    streakModIconIdx = mods.length;
                    newMods[streakModIconIdx] = is;
                    client.setModIcons(newMods);

                    installed = true;
                    retryTicks = 0;

                    // Rebuild prompt so <img=…> is parsed everywhere
                    client.runScript(ScriptID.CHAT_PROMPT_INIT);
                }
                catch (Exception ignored) { /* keep UX resilient */ }
            });
        });
    }
}
