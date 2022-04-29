package dev.zotware.roble.util.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.zotware.roble.RoblePlugin;
import dev.zotware.roble.exceptions.ItemBuildException;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Item extends ItemStack {

    private final ArrayList<ClickEvent> clickEvents;

    public Item(@NotNull Item item) {
        super(item);
        clickEvents = new ArrayList<>();
    }

    public Item(@NotNull ItemStack itemStack) {
        super(itemStack);
        clickEvents = new ArrayList<>();
    }

    public Item(@NotNull Object material, int amount) throws ItemBuildException {
        clickEvents = new ArrayList<>();
        create(material, amount);
    }

    public Item(@NotNull ConfigurationSection itemSection, @Nullable String... placeholders) throws ItemBuildException {
        clickEvents = new ArrayList<>();

        final String materialName = itemSection.getString("material");
        create(materialName, 1);

        String name = itemSection.getString("name");
        List<String> lore = itemSection.getStringList("lore");

        for (String placeholder : placeholders) {
            if (placeholder == null || !placeholder.contains(":")) continue;
            String[] args = placeholder.split(":");
            if (name != null) name = name.replace(args[0], args[1]);

            if (!lore.isEmpty()) for (int i = -1; ++i < lore.size(); )
                lore.set(i, lore.get(i).replace(args[0], args[1]));
        }

        if (name != null && !name.isEmpty()) setDisplayName(name);
        if (!lore.isEmpty()) setLore(lore);
    }

    // TAG constructor helpers

    private void create(@Nullable Object materialObject, int amount) throws ItemBuildException {
        final String nullMessage = "A material was not present (NULL or Empty).";
        if (materialObject == null) throw new ItemBuildException(nullMessage);

        String materialName;
        if (materialObject instanceof Material) materialName = ((Material) materialObject).name();
        else if (materialObject instanceof String) materialName = (String) materialObject;
        else throw new ItemBuildException("The passed material object was neither of type Material nor String.");

        if (materialName.isEmpty()) throw new ItemBuildException(nullMessage);

        if (materialName.toUpperCase().startsWith("HEAD:")) {
            final String[] args = materialName.split(":");
            final String value = args[1];

            setType(Material.PLAYER_HEAD);
            setAmount(amount);
            SkullMeta skullMeta = (SkullMeta) getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwner(value);
                setItemMeta(skullMeta);
            }

            return;
        } else if (materialName.toUpperCase().startsWith("TEXTURE:")) {
            final String[] args = materialName.split(":");
            final String value = args[1];

            setType(Material.PLAYER_HEAD);
            setAmount(amount);
            SkullMeta skullMeta = (SkullMeta) getItemMeta();
            if (skullMeta != null) {
                GameProfile profile = new GameProfile(UUID.randomUUID(), null);
                profile.getProperties().put("textures", new Property("textures", value));
                try {
                    Field field = skullMeta.getClass().getDeclaredField("profile");
                    field.setAccessible(true);
                    field.set(skullMeta, profile);
                } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {e.printStackTrace();}
                setItemMeta(skullMeta);
            }
            return;
        }

        materialName = materialName.toUpperCase().replace("_", "-");
        Material material = Material.getMaterial(materialName);
        if (material == null) material = Material.getMaterial(RoblePlugin.INSTANCE.predictCorrectValue(Material.class, materialName));
        if (material == null) throw new ItemBuildException("The material \"" + materialName + "\" was unable to be parsed (Even after prediction).");

        setType(material);
        setAmount(amount);
    }

    // TAG modifier methods

    public Item setDisplayName(@NotNull String name) {
        if (name.isEmpty()) return this;

        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) itemMeta.setDisplayName(RoblePlugin.INSTANCE.color(name));

        setItemMeta(itemMeta);
        return this;
    }

    public Item setLore(@NotNull String... lines) {
        if (lines == null || lines.length == 0) return this;

        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) itemMeta.setLore(new ArrayList<String>() {{
            for (String line : lines) add(RoblePlugin.INSTANCE.color(line));
        }});

        setItemMeta(itemMeta);
        return this;
    }

    public Item setLore(@NotNull List<String> lore) {
        if (lore.isEmpty()) return this;

        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) itemMeta.setLore(new ArrayList<String>() {{
            for (String line : lore) add(RoblePlugin.INSTANCE.color(line));
        }});

        setItemMeta(itemMeta);
        return this;
    }

    public Item addFlags(@NotNull ItemFlag... itemFlags) {
        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) itemMeta.addItemFlags(itemFlags);
        setItemMeta(itemMeta);
        return this;
    }

    public Item addEnchant(@NotNull Enchantment enchant, int level, boolean safeEnchant) {
        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) itemMeta.addEnchant(enchant, level, safeEnchant);
        setItemMeta(itemMeta);
        return this;
    }

    public Item removeFlags(@NotNull ItemFlag... itemFlags) {
        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) itemMeta.removeItemFlags(itemFlags);
        return this;
    }

    public Item removeEnchants(@NotNull Enchantment... enchants) {
        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) for (Enchantment enchantment : enchants)
            itemMeta.removeEnchant(enchantment);
        setItemMeta(itemMeta);
        return this;
    }

    public Item addPotionMeta(@NotNull Color color, @NotNull PotionEffect... potionEffects) {
        if (!getType().name().contains("POTION")) setType(Material.POTION);

        PotionMeta potionMeta = (PotionMeta) getItemMeta();
        if (potionMeta != null) {
            potionMeta.setColor(color);
            for (int i = -1; ++i < potionEffects.length; )
                potionMeta.addCustomEffect(potionEffects[i], true);
        }

        return this;
    }

    public Item setUnbreakable(boolean unbreakable) {
        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
            setItemMeta(itemMeta);
        }
        return this;
    }

    public boolean isInLore(String text) {
        final ItemMeta itemMeta = getItemMeta();
        if (itemMeta != null) return (itemMeta.getLore() != null && itemMeta.getLore().contains(text));
        return false;
    }

    /**
     * @param clickEvent The "ClickEvent" to tether to the item (all action in each event will operate upon the item being clicked in its respected interface).
     * @return The item object.
     */
    public Item addClickEvent(@NotNull ClickEvent clickEvent) {
        getClickEvents().add(clickEvent);
        return this;
    }

    // TAG getters & setters

    public ArrayList<ClickEvent> getClickEvents() {return clickEvents;}

    public interface ClickEvent {

        default boolean shouldCancelNow(@NotNull InventoryClickEvent e, @NotNull MenuInstance menuInstance, @NotNull Player player) {
            return (e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT
                    && e.getClick() != ClickType.SHIFT_LEFT && e.getClick() != ClickType.SHIFT_RIGHT);
        }

        /**
         * @param e            The "InventoryClickEvent" associated to the event operation.
         * @param menuInstance The menu instance the item click event will be tethered to.
         * @param player       The player who clicked the item.
         * @return Whether to cancel the original "InventoryClickEvent".
         */
        default boolean operate(@NotNull InventoryClickEvent e, @NotNull MenuInstance menuInstance, @NotNull Player player) {

            return true;
        }

    }

}