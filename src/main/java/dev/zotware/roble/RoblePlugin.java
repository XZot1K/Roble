package dev.zotware.roble;

import dev.zotware.roble.core.GlobalListeners;
import dev.zotware.roble.util.ReflectionHandler;
import dev.zotware.roble.util.gui.MenuInstance;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RoblePlugin extends JavaPlugin {

    public static final double SERVER_VERSION;
    public static final boolean HEX_VERSION;
    // TAG static variables
    public static RoblePlugin INSTANCE;

    // static construct
    static {
        SERVER_VERSION = Double.parseDouble(Bukkit.getServer().getClass().getPackage().getName()
                .replace(".", ",").split(",")[3]
                .replace("_R", ".").replaceAll("[rvV_]*", ""));
        HEX_VERSION = (Math.floor(SERVER_VERSION) >= 1_16);
    }

    // TAG handlers
    private ReflectionHandler reflectionHandler;
    private GlobalListeners globalListeners;
    // TAG storage
    private Map<UUID, MenuInstance> menuInstances;
    private Map<UUID, Map<String, Long>> cooldowns;
    // TAG helpers
    private Pattern hexPattern;
    private Random random;
    // TAG hooks
    private boolean papiInstalled;
    private Economy economy;

    @Override
    public void onEnable() {
        INSTANCE = this;
        random = new Random();
        menuInstances = new HashMap<>();
        cooldowns = new HashMap<>();

        papiInstalled = (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null);
        if (setupEconomy()) getServer().getLogger().info("Vault was found and hooked.");

        if (HEX_VERSION) hexPattern = Pattern.compile("#[a-fA-F\\d]{6}");
        else hexPattern = null;

        refreshReflectionHandler();
        setGlobalListeners(new GlobalListeners(this));

        enable();
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        disable();

        INSTANCE.getServer().getMessenger().unregisterIncomingPluginChannel(INSTANCE);
        INSTANCE.getServer().getMessenger().unregisterOutgoingPluginChannel(INSTANCE);
    }

    public abstract void enable();

    public abstract void disable();

    // TAG general

    /**
     * Updates the reflection handler by remaking the reflection storage.
     */
    public synchronized void refreshReflectionHandler() {
        if (reflectionHandler == null) {
            reflectionHandler = new ReflectionHandler();
            return;
        }

        getReflectionHandler().update();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();

        return true;
    }

    public void sendActionBar(@NotNull Player player, @NotNull String text, @Nullable String... placeholders) {
        final TextComponent textComponent = new TextComponent(color(applyPlaceholders(player, text, placeholders)));
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, textComponent);
    }

    /**
     * Sends a message to the player/command-sender (If prefixed with {bar} and the recipient is a player, the message goes to the action bar).
     *
     * @param recipient    The player/command-sender to send to.
     * @param message      The message with placeholders.
     * @param placeholders All placeholders in the format <placeholder>:<value>.
     */
    public void send(@NotNull CommandSender recipient, @NotNull String message, @Nullable String... placeholders) {
        if (message.isEmpty()) return;

        if (recipient instanceof Player) {
            final Player player = ((Player) recipient);
            if (!player.isOnline()) return;

            String newMessage = applyPlaceholders(player, message, placeholders);
            if (newMessage.toLowerCase().startsWith("{bar:")) {
                Pattern p = Pattern.compile("\\{bar:\\s+(\\d)}");
                Matcher matcher = p.matcher(newMessage);
                if (matcher.find() && !isNotNumeric(matcher.group())) {
                    final String finalMessage = newMessage.replace("{bar:" + matcher.group() + "}", "");
                    final int[] values = new int[2];

                    values[1] = Integer.parseInt(matcher.group());
                    values[0] = getServer().getScheduler().runTaskTimer(this, () -> {
                        if (values[1] <= 0) {
                            getServer().getScheduler().cancelTask(values[0]);
                            return;
                        }

                        sendActionBar(player, finalMessage);
                        values[1]++;
                    }, 0, 10).getTaskId();
                    return;
                }

                sendActionBar(player, message.substring(5));
            } else if (newMessage.toLowerCase().startsWith("{bar")) sendActionBar(player, newMessage.substring(5));
            else player.sendMessage(color(newMessage));
        } else recipient.sendMessage(color(message));
    }

    /**
     * @param message The message to translate.
     * @return The colored text.
     */
    public String color(@NotNull String message) {
        if (message.isEmpty()) return message;

        if (HEX_VERSION) {
            Matcher matcher = hexPattern.matcher(message);
            while (matcher.find()) {
                final ChatColor hexColor = ChatColor.of(matcher.group());
                final String before = message.substring(0, matcher.start()), after = message.substring(matcher.end());
                matcher = hexPattern.matcher(message = (before + hexColor + after));
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String applyPlaceholders(@NotNull String text, @Nullable String... placeholders) {
        if (text.isEmpty() || placeholders == null || placeholders.length <= 0)
            return text;

        for (int i = -1; ++i < placeholders.length; ) {
            final String placeholder = placeholders[i];
            if (placeholder == null || !placeholder.contains(":")) continue;

            final String[] args = placeholder.split(":");
            text = text.replaceAll("(?i)" + Pattern.quote(args[0]), args[1]);
        }

        return text;
    }

    /**
     * @param player       The player to associate PlaceholderAPI replacements with.
     * @param text         The text to apply replacements to.
     * @param placeholders The placeholders in the format <placeholder>:<replacement>.
     * @return The text with applied replacements.
     */
    public String applyPlaceholders(@NotNull Player player, @NotNull String text, @Nullable String... placeholders) {
        return applyPlaceholders((papiInstalled ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text) : text), placeholders);
    }

    /**
     * Compares all enumeration values to the passed value to attempt and correct it.
     *
     * @param enumeration The enumeration to use.
     * @param value       The value to compare.
     * @return The potential corrected value (Can return NULL).
     */
    public String predictCorrectValue(@NotNull Class<? extends Enum<?>> enumeration, @NotNull String value) {
        String currentWinner = ((enumeration.getEnumConstants().length > 0) ? enumeration.getEnumConstants()[0].name() : "");
        int currentDistance = 0;

        for (Enum<?> operation : enumeration.getEnumConstants()) {
            final int newDistance = StringUtils.getLevenshteinDistance(operation.name(), value);
            if (newDistance > currentDistance) {
                currentWinner = operation.name();
                currentDistance = newDistance;
            }
        }

        return currentWinner;
    }

    /**
     * Wraps a string into multiple lines based on a word count.
     *
     * @param text          The long string to wrap.
     * @param wordLineLimit The line size in terms of word count.
     * @return wraps the string to multiple lines
     */
    public List<String> wrapString(@NotNull String text, int wordLineLimit) {
        List<String> result = new ArrayList<>();
        final int longWordCount = getConfig().getInt("description-long-word-wrap");
        final String[] words = text.trim().split(" ");
        if (words.length > 0) {
            int wordCount = 0;
            StringBuilder sb = new StringBuilder();
            for (int i = -1; ++i < words.length; ) {
                String word = words[i];
                if (wordCount < wordLineLimit) {
                    if (word.length() >= longWordCount && longWordCount > 0)
                        word = word.substring(0, longWordCount);
                    sb.append(word).append(" ");
                    wordCount++;
                    continue;
                }

                result.add(sb.toString().trim());
                sb = new StringBuilder();
                sb.append(word).append(" ");
                wordCount = 1;
            }
            result.add(sb.toString().trim());
        }
        return result;
    }

    // TAG numerical

    /**
     * See if a string is NOT a numerical value.
     *
     * @param string The string to check.
     * @return Whether it is numerical or not.
     */
    public boolean isNotNumeric(@NotNull String string) {
        if (string.isEmpty()) return true;

        final char[] chars = string.toCharArray();
        if (chars.length == 1 && !Character.isDigit(chars[0])) return true;

        for (int i = -1; ++i < string.length(); ) {
            final char c = chars[i];
            if (!Character.isDigit(c) && c != '.' && !((i == 0 && c == '-'))) return true;
        }

        return false;
    }

    /**
     * Gets the roman numeral value of the passed integer.
     *
     * @param value The numerical value.
     * @return The roman numerical alternative.
     */
    public String getRomanNumeral(int value) {
        final String[] rnChars = {"M", "CM", "D", "C", "XC", "L", "X", "IX", "V", "I"};
        int[] values = {1000, 900, 500, 100, 90, 50, 10, 9, 5, 1};
        StringBuilder retVal = new StringBuilder();
        for (int i = -1; ++i < values.length; ) {
            int numberInPlace = (value / values[i]);
            if (numberInPlace == 0) continue;
            retVal.append((numberInPlace == 4 && i > 0) ? (rnChars[i] + rnChars[i - 1])
                    : new String(new char[numberInPlace]).replace("\0", rnChars[i]));
            value = (value % values[i]);
        }
        return retVal.toString();
    }

    /**
     * Gets a random integer in a given range.
     *
     * @param minimumValue The minimum value.
     * @param maximumValue The maximum value.
     * @return The random integer.
     */
    public long getRandomBetween(long minimumValue, long maximumValue) {
        return getRandom().nextLong() % (maximumValue - minimumValue) + maximumValue;
    }

    /**
     * Gets a random integer in a given range.
     *
     * @param minimumValue The minimum value.
     * @param maximumValue The maximum value.
     * @return The random integer.
     */
    public int getRandomBetween(int minimumValue, int maximumValue) {
        return ((int) (Math.random() * ((maximumValue - minimumValue) + 1)) + minimumValue);
    }

    /**
     * Gets a random double in a given range.
     *
     * @param minimumValue The minimum value.
     * @param maximumValue The maximum value.
     * @return The random double.
     */
    public double getRandomBetween(double minimumValue, double maximumValue) {
        return minimumValue + (maximumValue - minimumValue) * getRandom().nextDouble();
    }

    // TAG utility

    /**
     * Executes a list of commands and determines how their executed with placeholder support.
     *
     * @param commandList  A list of commands with the option to add :CONSOLE, :CHAT, or :PLAYER as a suffix.
     * @param player       The player to use for the player placeholder (Can be set to NULL).
     * @param placeholders The placeholders to replace using the format <placeholder>:<value>.
     */
    public void executeCommands(@NotNull List<String> commandList, @NotNull Player player, @Nullable String... placeholders) {
        if (commandList.isEmpty()) return;

        List<String> fixedPlaceholders = new ArrayList<>();
        if (placeholders != null && placeholders.length > 0) fixedPlaceholders.addAll(Arrays.asList(placeholders));
        if (!fixedPlaceholders.contains("{player}:" + player.getName()))
            fixedPlaceholders.add("{player}:" + player.getName());
        if (!fixedPlaceholders.contains("{uuid}:" + player.getUniqueId()))
            fixedPlaceholders.add("{uuid}:" + player.getUniqueId());

        String[] newPlaceholderList = new String[fixedPlaceholders.size()];
        newPlaceholderList = fixedPlaceholders.toArray(newPlaceholderList);

        for (String commandLine : commandList) {
            if (commandLine == null || commandLine.isEmpty()) continue;
            if (commandLine.contains(":")) {
                final String[] commandArgs = commandLine.split(":");
                if (commandArgs[1].equalsIgnoreCase("CHAT")) {
                    player.chat(applyPlaceholders(player, commandArgs[0].replaceAll("(?i):CHAT", ""), newPlaceholderList));
                } else getServer().dispatchCommand(commandArgs[1].equalsIgnoreCase("PLAYER")
                        ? player : getServer().getConsoleSender(), applyPlaceholders(player, ((commandLine.startsWith("/")
                        ? commandArgs[0].substring(1) : commandArgs[0]).replaceAll("(?i):PLAYER", "")
                        .replaceAll("(?i):CONSOLE", "")), newPlaceholderList));
            } else getServer().dispatchCommand(getServer().getConsoleSender(),
                    applyPlaceholders(player, commandLine, newPlaceholderList));
        }
    }

    /**
     * Executes a list of commands and determines how their executed with placeholder support.
     *
     * @param commandList  A list of commands sent by the console.
     * @param placeholders The placeholders to replace using the format <placeholder>:<value>.
     */
    public void executeCommands(@NotNull List<String> commandList, @Nullable String... placeholders) {
        if (commandList.isEmpty()) return;

        final List<String> fixedPlaceholders = ((placeholders == null || placeholders.length <= 0) ? new ArrayList<>() : Arrays.asList(placeholders.clone()));
        final String[] newPlaceholderList = new String[fixedPlaceholders.size()];

        for (String commandLine : commandList) {
            if (commandLine == null || commandLine.isEmpty()) continue;
            getServer().dispatchCommand(getServer().getConsoleSender(), applyPlaceholders(commandLine, newPlaceholderList));
        }
    }

    /**
     * Gets the modifier from a permission node a player has.
     *
     * @param player The player to check for.
     * @return The modifier found in their nodes.
     */
    public double getModifierFromPermission(@NotNull Player player, @NotNull String permissionPrefix) {
        double currentModifier = 1;
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
            if (permissionAttachmentInfo.getPermission().toLowerCase().startsWith(permissionPrefix)) {
                final String intValue = permissionAttachmentInfo.getPermission().toLowerCase().replace(permissionPrefix, "");
                if (isNotNumeric(intValue)) continue;

                final double tempValue = Double.parseDouble(permissionAttachmentInfo.getPermission().toLowerCase().replace(permissionPrefix, ""));
                if (tempValue > currentModifier) currentModifier = tempValue;
            }
        }

        return currentModifier;
    }

    /**
     * Gets the limit from a permission node a player has.
     *
     * @param player The player to check for.
     * @return The limit found in their nodes.
     */
    public int getLimitFromPermission(@NotNull Player player, @NotNull String permissionPrefix) {
        if (player.hasPermission(permissionPrefix + "*")) return -1;
        int foundLimit = 0;
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
            if (permissionAttachmentInfo.getPermission().toLowerCase().startsWith(permissionPrefix)) {
                String intValue = permissionAttachmentInfo.getPermission().toLowerCase().replace(permissionPrefix, "");
                if (isNotNumeric(intValue)) continue;

                int tempValue = Integer.parseInt(permissionAttachmentInfo.getPermission().toLowerCase().replace(permissionPrefix, ""));
                if (tempValue > foundLimit) foundLimit = tempValue;
            }
        }

        return foundLimit;
    }

    /**
     * Gets a list of all online player's names.
     *
     * @return The list of player names
     */
    public List<String> getOnlinePlayerNames() {
        return new ArrayList<String>() {{
            getServer().getOnlinePlayers().forEach(player -> add(player.getName()));
        }};
    }

    /**
     * Creates a text-based progression bar.
     *
     * @param currentValue The current value.
     * @param neededValue  The required value to reach 100%.
     * @param segments     How many segments (default should be 12).
     * @param filledColor  The filled progression color.
     * @param emptyColor   The empty progression color.
     * @return The full created progression bar string.
     */
    public String getBar(double currentValue, double neededValue, int segments, @NotNull String segmentCharacter,
                         @NotNull ChatColor filledColor, @NotNull ChatColor emptyColor) {
        final StringBuilder bar = new StringBuilder();
        double fractionValue = (currentValue / neededValue) * segments;

        final String sc = (segmentCharacter.isEmpty() ? "\u2022" : segmentCharacter);
        for (int i = -1; ++i < segments; ) {
            if (fractionValue > 0) {
                bar.append(filledColor).append(sc);
                fractionValue -= 1;
            } else bar.append(emptyColor).append(sc);
        }

        return bar.toString();
    }

    // TAG cooldowns

    /**
     * @param playerIdentifier The player to check the cooldown for (UUID or Player).
     * @param cooldownId       The id of the cooldown to check.
     * @param preSetCooldown   The cooldown duration to set (used for loading in cooldowns).
     */
    public void updateCooldown(@NotNull Object playerIdentifier, @NotNull String cooldownId, long... preSetCooldown) {
        UUID playerUniqueId = null;
        if (playerIdentifier instanceof UUID) playerUniqueId = (UUID) playerIdentifier;
        else if (playerIdentifier instanceof Player) playerUniqueId = ((Player) playerIdentifier).getUniqueId();

        if (playerUniqueId != null) {
            final Map<String, Long> cds = getCooldowns().getOrDefault(playerUniqueId, new HashMap<>());
            cds.put(cooldownId, ((preSetCooldown.length > 0) ? preSetCooldown[0] : System.currentTimeMillis()));
            getCooldowns().put(playerUniqueId, cds);
        }
    }

    /**
     * @param playerIdentifier The player to check the cooldown for (UUID or Player).
     * @param cooldownId       The id of the cooldown to check.
     * @param cooldown         The cooldown duration.
     * @return Whether the player is currently on cooldown.
     */
    public boolean onCooldown(@NotNull Object playerIdentifier, @NotNull String cooldownId, int cooldown) {
        return (getCooldown(playerIdentifier, cooldownId, cooldown) > 0);
    }

    /**
     * @param playerIdentifier The player to check the cooldown for (UUID or Player).
     * @param cooldownId       The id of the cooldown to check.
     * @param cooldown         The cooldown duration.
     * @return Whether the player is currently on cooldown.
     */
    public long getCooldown(@NotNull Object playerIdentifier, @NotNull String cooldownId, int cooldown) {
        UUID playerUniqueId = null;
        if (playerIdentifier instanceof UUID) playerUniqueId = (UUID) playerIdentifier;
        else if (playerIdentifier instanceof Player) playerUniqueId = ((Player) playerIdentifier).getUniqueId();

        if (playerUniqueId != null) {
            final Map<String, Long> cds = getCooldowns().getOrDefault(playerUniqueId, null);
            if (cds != null && cds.containsKey(cooldownId))
                return ((cds.get(cooldownId) / 1000) + cooldown) - (System.currentTimeMillis() / 1000);
        }

        return 0;
    }

    /**
     * @param playerUniqueId The player's unique id.
     * @return All the player's cooldowns in a single string (Format: <cd>:<time-stamp>,<cd>:<time-stamp>,...).
     */
    public String getCooldownString(@NotNull UUID playerUniqueId) {
        final Map<String, Long> cds = getCooldowns().getOrDefault(playerUniqueId, null);
        if (cds == null || cds.isEmpty()) return null;

        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : cds.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }

        return sb.toString();
    }

    /**
     * @param playerUniqueId The player to check the cooldown for (UUID or Player).
     * @param section        The configuration section from a YAML file.
     * @param path           The path in the configuration file of the cooldown string.
     */
    public void loadCooldowns(@NotNull UUID playerUniqueId, @NotNull ConfigurationSection section, @NotNull String path) {
        final String cooldownString = section.getString(path);
        if (cooldownString == null || cooldownString.isEmpty()) return;
        loadCooldowns(playerUniqueId, cooldownString);
    }

    /**
     * @param playerUniqueId The player to check the cooldown for (UUID or Player).
     * @param cooldownString The cooldown string to read.
     */
    public void loadCooldowns(@NotNull UUID playerUniqueId, @NotNull String cooldownString) {
        if (cooldownString.contains(","))
            for (String cooldown : cooldownString.split(",")) {
                final String[] args = cooldown.split(":");
                updateCooldown(playerUniqueId, args[0], Long.parseLong(args[1]));
            }
        else {
            final String[] args = cooldownString.split(":");
            updateCooldown(playerUniqueId, args[0], Long.parseLong(args[1]));
        }
    }

    // TAG menu instances

    /**
     * @param player The player to get the instance from.
     * @return The menu instance or null.
     */
    public MenuInstance getMenuInstance(@NotNull Player player) {return menuInstances.getOrDefault(player.getUniqueId(), null);}

    /**
     * Updates the player's stored menu instance.
     *
     * @param player The player to get the instance from.
     */
    public void updateMenuInstance(@NotNull Player player, @NotNull MenuInstance menuInstance) {menuInstances.put(player.getUniqueId(), menuInstance);}

    public void clearMenuInstance(@NotNull Player player) {menuInstances.remove(player.getUniqueId());}

    // TAG getters & setters

    public ReflectionHandler getReflectionHandler() {return reflectionHandler;}

    public Random getRandom() {return random;}

    public Economy getEconomy() {return economy;}

    public Map<UUID, Map<String, Long>> getCooldowns() {return cooldowns;}

    public GlobalListeners getGlobalListeners() {return globalListeners;}

    public void setGlobalListeners(GlobalListeners globalListeners) {this.globalListeners = globalListeners;}

}