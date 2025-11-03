package com.birdwatcher52.ministreak;

import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.WidgetNode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Queue;

/** Thin wrapper on InterfaceID 660 (“Notification Display”). */
@Singleton
final class ToastService
{
    private static final int INTERFACE_ID = 660;
    // Same script the client uses to populate title/message/color
    private static final int SCRIPT_ID = 3343; // NOTIFICATION_DISPLAY_INIT

    private static final int GROUP_RESIZABLE_CLASSIC = 161;
    private static final int GROUP_RESIZABLE_MODERN  = 164;
    private static final int GROUP_FIXED_CLASSIC     = 548;

    private static final int CHILD_CONTAINER_INDEX   = 13; // matches where 660 mounts in each layout
    private static final int CHILD_FIXED_INDEX       = 42;

    private final Queue<Item> queue = new ArrayDeque<>();

    @Inject private Client client;
    @Inject private ClientThread clientThread;

    void enqueue(String title, String message, Color color)
    {
        queue.add(new Item(title, message, color));
    }

    /** Call this every tick to pump one toast (when 660 is not already open). */
    void processQueue()
    {
        // If 660 is already up, wait.
        if (client.getWidget(INTERFACE_ID, 1) != null)
        {
            return;
        }
        final Item next = queue.poll();
        if (next == null) return;

        // Open 660 under the correct parent container for the current layout.
        final int parent = getParentComponentId();
        final WidgetNode node = client.openInterface(parent, INTERFACE_ID, WidgetModalMode.MODAL_CLICKTHROUGH);

        // Encode RGB without alpha (client expects 24-bit int; -1 means default/white).
        final int rgb = (next.color == null) ? -1 : ((next.color.getRed() & 0xFF) << 16)
                | ((next.color.getGreen() & 0xFF) << 8)
                | (next.color.getBlue() & 0xFF);

        // Populate content
        client.runScript(SCRIPT_ID, next.title, next.message, rgb);

        // Close after it animates out (width goes to 0).
        clientThread.invokeLater(() -> {
            final Widget w = client.getWidget(INTERFACE_ID, 1);
            if (w == null || w.getWidth() > 0)
            {
                return false; // re-check next tick until closed
            }
            client.closeInterface(node, true);
            return true;
        });
    }

    private int getParentComponentId()
    {
        final boolean resized = client.isResized();
        if (resized)
        {
            final boolean modern = client.getVarbitValue(Varbits.SIDE_PANELS) == 1;
            final int group = modern ? GROUP_RESIZABLE_MODERN : GROUP_RESIZABLE_CLASSIC;
            return WidgetUtil.packComponentId(group, CHILD_CONTAINER_INDEX);
        }
        return WidgetUtil.packComponentId(GROUP_FIXED_CLASSIC, CHILD_FIXED_INDEX);
    }

    private static final class Item
    {
        final String title, message; final Color color;
        Item(String t, String m, Color c) { title = t; message = m; color = c; }
    }
}
