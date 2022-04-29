package dev.zotware.roble.util.gui;

import dev.zotware.roble.RoblePlugin;
import dev.zotware.roble.util.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MenuInstance {

    private final Inventory inventory;
    private final String title;
    private final HashMap<UUID, Page> currentPageMap;
    private Page firstPage;
    private ItemStack backgroundItem;
    private Pair<Integer, ItemStack> nextPageItem, previousPageItem;

    private List<Integer> slots;

    public MenuInstance(@NotNull String title, @Nullable InventoryHolder inventoryHolder, @NotNull InventoryType inventoryType, int... pageSlots) {
        currentPageMap = new HashMap<>();
        setFirstPage(null);
        inventory = RoblePlugin.INSTANCE.getServer().createInventory(inventoryHolder, inventoryType, (this.title = RoblePlugin.INSTANCE.color(title)));
        setSlots(new ArrayList<Integer>() {{
            for (int slot : pageSlots) add(slot);
        }});
    }

    public MenuInstance(@NotNull String title, @Nullable InventoryHolder inventoryHolder, int size, int... pageSlots) {
        currentPageMap = new HashMap<>();
        setFirstPage(null);
        inventory = RoblePlugin.INSTANCE.getServer().createInventory(inventoryHolder, size, (this.title = RoblePlugin.INSTANCE.color(title)));
        setSlots(new ArrayList<Integer>() {{
            for (int slot : pageSlots) add(slot);
        }});
    }

    public MenuInstance(@NotNull String title, int size, int... pageSlots) {
        currentPageMap = new HashMap<>();
        setFirstPage(null);
        inventory = RoblePlugin.INSTANCE.getServer().createInventory(null, size, (this.title = RoblePlugin.INSTANCE.color(title)));
        setSlots(new ArrayList<Integer>() {{
            for (int slot : pageSlots) add(slot);
        }});
    }

    public boolean isSame(@NotNull InventoryView inventoryView) {return inventoryView.getTitle().equals(getTitle());}

    public void clearSlots() {
        for (int slot : getSlots()) {
            if (slot >= 0 && slot < getInventory().getSize())
                getInventory().setItem(slot, null);
        }
    }

    public void applyBackground() {
        if (getBackgroundItem() != null)
            for (int i = -1; ++i < inventory.getSize(); ) {
                if (slots.contains(i)) continue;

                final ItemStack itemStack = getInventory().getItem(i);
                if (itemStack == null) continue;

                getInventory().setItem(i, getBackgroundItem());
            }
    }

    public void open(@NotNull Player player) {
        if (getInventory() == null) return;

        final Page currentPage = getCurrentPage(player.getUniqueId());
        if (currentPage != null) load(player, currentPage);
        else if (getFirstPage() != null) load(player, getFirstPage());

        applyBackground();
        player.openInventory(getInventory());
        RoblePlugin.INSTANCE.updateMenuInstance(player, this);
    }

    public void load(@NotNull Player player, @NotNull Page page) {
        setCurrentPage(player.getUniqueId(), page);
        clearSlots();

        for (Map.Entry<Integer, Item> contentEntry : page.getContents().entrySet())
            if (contentEntry.getKey() >= 0 && contentEntry.getKey() < getInventory().getSize())
                getInventory().setItem(contentEntry.getKey(), contentEntry.getValue());

        if (hasPreviousPage() && getPreviousPageItem() != null)
            getInventory().setItem(getPreviousPageItem().getKey(), getPreviousPageItem().getValue());
        else if (getPreviousPageItem() != null && getBackgroundItem() != null)
            getInventory().setItem(getPreviousPageItem().getKey(), getBackgroundItem());

        if (hasNextPage() && getNextPageItem() != null)
            getInventory().setItem(getNextPageItem().getKey(), getNextPageItem().getValue());
        else if (getNextPageItem() != null && getBackgroundItem() != null)
            getInventory().setItem(getPreviousPageItem().getKey(), getBackgroundItem());

        player.updateInventory();
    }

    /**
     * @param indexPage The page to insert the passed page before.
     * @param page      The page to insert.
     */
    public void insertPage(@NotNull Page indexPage, @NotNull Page page) {
        if (indexPage.getPreviousPage() != null) {
            final Page previousPage = indexPage.getPreviousPage();
            previousPage.setNextPage(page);
            indexPage.setPreviousPage(page);
            return;
        }

        indexPage.setPreviousPage(page);
    }

    public void addPage(@NotNull Page page) {
        if (getFirstPage() == null) {
            setFirstPage(page);
            return;
        }

        Page currentPage = getFirstPage();
        while (currentPage.getNextPage() != null)
            currentPage = currentPage.getNextPage();

        currentPage.setNextPage(page);
        page.setPreviousPage(currentPage);
    }

    public boolean hasNextPage() {return (getFirstPage() != null && getFirstPage().getNextPage() != null);}

    public boolean hasPreviousPage() {return (getFirstPage() != null && getFirstPage().getPreviousPage() != null);}


    // TAG getters & setters

    public Page getCurrentPage(@NotNull UUID playerUniqueId) {return getCurrentPageMap().getOrDefault(playerUniqueId, null);}

    public void setCurrentPage(@NotNull UUID playerUniqueId, @NotNull Page currentPage) {getCurrentPageMap().put(playerUniqueId, currentPage);}

    public Inventory getInventory() {return inventory;}

    public ItemStack getBackgroundItem() {return backgroundItem;}

    public void setBackgroundItem(@NotNull ItemStack backgroundItem) {this.backgroundItem = backgroundItem;}

    public Pair<Integer, ItemStack> getNextPageItem() {return nextPageItem;}

    public void setNextPageItem(int slot, @NotNull ItemStack nextPageItem) {
        if (this.nextPageItem != null) this.nextPageItem.update(slot, nextPageItem);
        else this.nextPageItem = new Pair<>(slot, nextPageItem);
    }

    public Pair<Integer, ItemStack> getPreviousPageItem() {return previousPageItem;}

    public void setPreviousPageItem(int slot, @NotNull ItemStack previousPageItem) {
        if (this.previousPageItem != null) this.previousPageItem.update(slot, previousPageItem);
        else this.previousPageItem = new Pair<>(slot, previousPageItem);
    }

    public List<Integer> getSlots() {return slots;}

    public void setSlots(List<Integer> slots) {this.slots = slots;}

    public HashMap<UUID, Page> getCurrentPageMap() {return currentPageMap;}

    public Page getFirstPage() {return firstPage;}

    public void setFirstPage(Page firstPage) {this.firstPage = firstPage;}

    public String getTitle() {return title;}


    public static class Page {

        private final Map<Integer, Item> contents;
        private Page previousPage, nextPage;

        public Page(@Nullable Item... items) {
            contents = new HashMap<>();
            setPreviousPage(null);
            setNextPage(null);

            if (items != null && items.length > 0)
                for (int i = -1; ++i < items.length; ) {
                    final Item item = items[i];
                    if (item == null) continue;
                    contents.put(i, item);
                }
        }

        public Page(@NotNull Map<Integer, Item> contents) {
            this.contents = contents;
            setPreviousPage(null);
            setNextPage(null);
        }

        public Page(@NotNull List<Pair<Integer, Item>> contentPairs) {
            this.contents = new HashMap<>();
            setPreviousPage(null);
            setNextPage(null);

            for (Pair<Integer, Item> pair : contentPairs)
                getContents().put(pair.getKey(), pair.getValue());
        }

        // TAG getters & setters

        public Page getNextPage() {return nextPage;}

        public void setNextPage(Page nextPage) {this.nextPage = nextPage;}

        public Page getPreviousPage() {return previousPage;}

        public void setPreviousPage(Page previousPage) {this.previousPage = previousPage;}

        public Map<Integer, Item> getContents() {return contents;}

    }

}