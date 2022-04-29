package dev.zotware.roble.util;

import dev.zotware.roble.RoblePlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ReflectionHandler {

    private final Map<String, Class<?>> rClassMap;
    private final Map<String, Method> methodMap;

    public ReflectionHandler() {
        rClassMap = new HashMap<>();
        methodMap = new HashMap<>();
    }

    /**
     * Clears all maps and re-makes the reflection class storage.
     */
    public void update() {
        if (!rClassMap.isEmpty()) rClassMap.clear();
        if (!methodMap.isEmpty()) methodMap.clear();

        try {
            final boolean beforeNBTPackageMove = (Math.floor(RoblePlugin.SERVER_VERSION) < 1_17);
            final String versionString = String.valueOf(RoblePlugin.SERVER_VERSION),
                    packageVersion = ("v" + versionString.charAt(0) + "_" + versionString.substring(1)).replace(".", "_R"),
                    nbtPackage = (beforeNBTPackageMove ? ("server." + packageVersion) : "nbt"),
                    worldItemPackage = (beforeNBTPackageMove ? ("server." + packageVersion) : "world.item");

            final Class<?> nbtTagCompound = Class.forName("net.minecraft." + nbtPackage + ".NBTTagCompound"),
                    craftItemStack = Class.forName("org.bukkit.craftbukkit." + packageVersion + ".inventory.CraftItemStack"),
                    itemStack = Class.forName("net.minecraft." + worldItemPackage + ".ItemStack"),
                    craftServer = Class.forName("org.bukkit.craftbukkit." + packageVersion + ".CraftServer"),
                    craftEntity = Class.forName("org.bukkit.craftbukkit." + packageVersion + ".entity.CraftEntity");

            rClassMap.put("NBTTagCompound", nbtTagCompound);
            rClassMap.put("CraftItemStack", craftItemStack);
            rClassMap.put("ItemStack", itemStack);
            rClassMap.put("NBTCompressedStreamTools", Class.forName("net.minecraft." + nbtPackage + ".NBTCompressedStreamTools"));
            rClassMap.put("NBTReadLimiter", Class.forName("net.minecraft." + nbtPackage + ".NBTReadLimiter"));
            rClassMap.put("CraftEntity", craftEntity);
            rClassMap.put("CraftServer", craftServer);
            rClassMap.put("IChatBaseComponent", Class.forName("net.minecraft.server." + nbtPackage + ".IChatBaseComponent"));
            rClassMap.put("PacketPlayOutChat", Class.forName("net.minecraft.server." + nbtPackage + ".PacketPlayOutChat"));
            rClassMap.put("CraftPlayer", Class.forName("net.minecraft.server." + nbtPackage + ".entity.CraftPlayer"));

            if (Math.floor(RoblePlugin.SERVER_VERSION) >= 1_17) {
                rClassMap.put("CraftWorld", Class.forName("org.bukkit.craftbukkit." + packageVersion + ".inventory.CraftItemStack"));
                rClassMap.put("CraftPlayer", Class.forName("org.bukkit.craftbukkit." + packageVersion + ".entity.CraftPlayer"));
                rClassMap.put("EntityArmorStand", Class.forName("net.minecraft.world.entity.decoration.EntityArmorStand"));
                rClassMap.put("EntityItem", Class.forName("net.minecraft.world.entity.item.EntityItem"));
            } else {
                rClassMap.put("EntityArmorStand", Class.forName("net.minecraft.server." + packageVersion + ".EntityArmorStand"));
                rClassMap.put("EntityItem", Class.forName("net.minecraft.server." + packageVersion + ".EntityItem"));
            }

            methodMap.put("getEntity", craftEntity.getDeclaredMethod("getEntity", craftServer, Entity.class));
            methodMap.put("asNMSCopy", craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class));
            methodMap.put("asBukkitCopy", craftItemStack.getDeclaredMethod("asBukkitCopy", itemStack));
            methodMap.put("getTag", craftItemStack.getDeclaredMethod(RoblePlugin.SERVER_VERSION >= 1_18 ? "u" : "getTag"));
            methodMap.put("getString", nbtTagCompound.getDeclaredMethod(RoblePlugin.SERVER_VERSION >= 1_18 ? "l" : "getString", String.class));
            methodMap.put("setString", nbtTagCompound.getDeclaredMethod((RoblePlugin.SERVER_VERSION >= 1_18 ? "a" : "setString"), String.class, String.class));
            methodMap.put("save", itemStack.getDeclaredMethod((RoblePlugin.SERVER_VERSION == 1_18.1 ? "a"
                    : (RoblePlugin.SERVER_VERSION > 1_18.1 ? "c" : "save")), nbtTagCompound));

            for (Method method : itemStack.getMethods()) {
                if (method.getReturnType().equals(nbtTagCompound) && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].equals(nbtTagCompound)) {
                    methodMap.put("JSON", method);
                    break;
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            RoblePlugin.INSTANCE.getLogger().log(Level.WARNING, "Unable to retrieve a NMS class used for NBT data (" + e.getMessage() + ").");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            RoblePlugin.INSTANCE.getLogger().log(Level.WARNING, "Unable to retrieve a method from a NMS class (" + e.getMessage() + ").");
        }
    }

    // general functions
    public Class<?> getClass(@NotNull String className) {return rClassMap.getOrDefault(className, null);}

    public Method getMethod(@NotNull String methodName) {return methodMap.getOrDefault(methodName, null);}

    // NBT functions
    public String getNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag) {
        try {
            Class<?> itemStackClass = getClass("ItemStack");
            Object craftItemStack = getMethod("asNMSCopy").invoke(itemStackClass, itemStack),
                    tagCompound = getMethod("getTag").invoke(craftItemStack);
            return (String) getMethod("getString").invoke(tagCompound, nbtTag);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ItemStack updateNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag, @NotNull String value) {
        try {
            Class<?> itemStackClass = getClass("ItemStack");
            Object craftItemStack = getMethod("asNMSCopy").invoke(itemStackClass, itemStack),
                    tagCompound = getMethod("getTag").invoke(craftItemStack);
            if (tagCompound == null) {
                Constructor<?> cons = getClass("NBTTagCompound").getConstructor();
                tagCompound = cons.newInstance();
            }

            getMethod("setString").invoke(tagCompound, nbtTag, value);
            getMethod("save").invoke(craftItemStack, tagCompound);

            return (ItemStack) getMethod("asBukkitCopy").invoke(craftItemStack.getClass(), craftItemStack);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {e.printStackTrace();}
        return itemStack;
    }

    // item serialization functions

    /**
     * Convert an item to a string including all NBT.
     *
     * @param itemStack The item to serialize.
     * @return The serialized string.
     */
    public String toString(@NotNull ItemStack itemStack) {
        YamlConfiguration itemConfig = new YamlConfiguration();
        itemConfig.set("item", itemStack);
        return itemConfig.saveToString().replace("'", "[sq]").replace("\"", "[dq]");
    }

    /**
     * Convert a string back into an item including all NBT.
     *
     * @param itemString The serialized item string.
     * @return The itemstack object.
     */
    public ItemStack toItem(@NotNull String itemString) {
        if (!itemString.contains("item:"))
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(itemString, 32).toByteArray());
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                Class<?> nbtReadLimiter = getClass("NBTReadLimiter"), itemStack = getClass("ItemStack"),
                        nbtTagCompound = getClass("NBTTagCompound");
                Object tag = getClass("NBTCompressedStreamTools").getMethod("a", DataInput.class, nbtReadLimiter).invoke(null, dataInputStream,
                        nbtReadLimiter.cast(nbtReadLimiter.getField("a").get(nbtReadLimiter)));

                if (!(Math.floor(RoblePlugin.SERVER_VERSION) <= 1_8)) {
                    Constructor<?> constructor = itemStack.getDeclaredConstructor(nbtTagCompound);
                    constructor.setAccessible(true);
                    Object craftItemStack = constructor.newInstance(tag);
                    return ((ItemStack) getMethod("asBukkitCopy").invoke(null, craftItemStack));
                }

                dataInputStream.close();
                inputStream.close();
                Object craftItemStack = itemStack.getMethod("createStack", nbtTagCompound).invoke(null, tag);
                return (ItemStack) getMethod("asBukkitCopy").invoke(null, craftItemStack);
            } catch (Exception e) {e.printStackTrace();}

        YamlConfiguration restoreConfig = new YamlConfiguration();
        try {
            restoreConfig.loadFromString(itemString.replace("[sq]", "'").replace("[dq]", "\""));
        } catch (InvalidConfigurationException ex) {
            ex.printStackTrace();
        }
        return restoreConfig.getItemStack("item");
    }

    /**
     * Convert an item to a JSON string format for in-game messages.
     *
     * @param itemStack The item to convert.
     * @return The JSON string (can return 'NULL').
     */
    public String toJSON(@NotNull ItemStack itemStack) {
        try {
            Constructor<?> nbtTagCompoundConstructor = getClass("NBTTagCompound").getConstructor();
            Object nbtTagCompound = nbtTagCompoundConstructor.newInstance();
            Object nmsStack = getMethod("asNMSCopy").invoke(null, itemStack);
            return getMethod("JSON").invoke(nmsStack, nbtTagCompound).toString();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

}