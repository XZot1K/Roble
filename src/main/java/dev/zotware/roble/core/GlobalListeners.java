package dev.zotware.roble.core;

import dev.zotware.roble.RoblePlugin;
import dev.zotware.roble.util.gui.Item;
import dev.zotware.roble.util.gui.MenuInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class GlobalListeners implements Listener {

    private final RoblePlugin INSTANCE;

    public GlobalListeners(@NotNull RoblePlugin instance) {
        this.INSTANCE = instance;
        INSTANCE.getServer().getPluginManager().registerEvents(this, INSTANCE);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        final Player player = (Player) e.getWhoClicked();

        final MenuInstance menuInstance = INSTANCE.getMenuInstance(player);
        if (menuInstance == null) return;

        final MenuInstance.Page page = menuInstance.getCurrentPage(player.getUniqueId());
        if (page == null) return;

        final Item item = page.getContents().getOrDefault(e.getSlot(), null);
        if (item == null) return;

        item.getClickEvents().forEach(event -> {
            final boolean shouldCancel = event.shouldCancelNow(e, menuInstance, player);
            if (shouldCancel) e.setCancelled(true);
            else e.setCancelled(event.operate(e, menuInstance, player));
        });
    }

    @EventHandler
    public void onExit(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            final Player player = (Player) e.getPlayer();
            final MenuInstance menuInstance = INSTANCE.getMenuInstance(player);
            if (menuInstance != null && player.getOpenInventory().getType() == InventoryType.CRAFTING)
                INSTANCE.clearMenuInstance(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {INSTANCE.clearMenuInstance(e.getPlayer());}

}