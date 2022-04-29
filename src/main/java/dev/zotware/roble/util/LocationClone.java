package dev.zotware.roble.util;

import dev.zotware.roble.RoblePlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class LocationClone implements Serializable {

    public static final long serialVersionUID = 0L;

    private String worldName;
    private double x, y, z;
    private float yaw, pitch;

    public <T> LocationClone(@NotNull T locationData) {
        if (locationData instanceof Location) {
            Location location = (Location) locationData;
            setWorldName(Objects.requireNonNull(location.getWorld()).getName());
            setX(location.getX());
            setY(location.getY());
            setZ(location.getZ());
            setYaw(location.getYaw());
            setPitch(location.getPitch());
            return;
        } else if (locationData instanceof String) {
            String locationString = (String) locationData;
            if (locationString.contains(",")) {
                final String[] args = locationString.split(",");
                setWorldName(args[0]);
                setX(Double.parseDouble(args[1]));
                setY(Double.parseDouble(args[2]));
                setZ(Double.parseDouble(args[3]));
                setYaw(Float.parseFloat(args[4]));
                setPitch(Float.parseFloat(args[5]));
                return;
            }
        }

        final Optional<World> findWorld = RoblePlugin.INSTANCE.getServer().getWorlds().stream().findAny();
        setWorldName(findWorld.map(WorldInfo::getName).orElse(""));
        setX(0);
        setY(0);
        setZ(0);
        setYaw(0F);
        setPitch(0F);
    }

    public LocationClone(@NotNull String worldName, double x, double y, double z, float yaw, float pitch) {
        setWorldName(worldName);
        setX(x);
        setY(y);
        setZ(z);
        setYaw(yaw);
        setPitch(pitch);
    }

    public <T> boolean isSame(@NotNull T location, boolean checkRotation) {
        if (location instanceof Location) {
            Location loc = (Location) location;
            return (loc.getWorld() != null && sameCalculation(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), checkRotation));
        } else if (location instanceof LocationClone) {
            LocationClone loc = (LocationClone) location;
            return sameCalculation(loc.getWorldName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), checkRotation);
        }
        return false;
    }

    private boolean sameCalculation(@Nullable String worldName, double x, double y, double z, float yaw, float pitch, boolean checkRotation) {
        return (worldName != null && worldName.equalsIgnoreCase(getWorldName()) && x == getX() && y == getY()
                && z == getZ() && (!checkRotation || (yaw == getYaw() && pitch == getPitch())));
    }

    public <T> double distance(@NotNull T location, boolean checkYAxis) {
        if (location instanceof Location) {
            Location loc = (Location) location;
            return distanceCalculation(loc.getX(), loc.getY(), loc.getZ(), checkYAxis);
        } else if (location instanceof LocationClone) {
            LocationClone loc = (LocationClone) location;
            return distanceCalculation(loc.getX(), loc.getY(), loc.getZ(), checkYAxis);
        }
        return -1;
    }

    private double distanceCalculation(double x, double y, double z, boolean checkYAxis) {
        final double highX = Math.max(getX(), x), highY = Math.max(getY(), y), highZ = Math.max(getZ(), z),
                lowX = Math.min(getX(), x), lowY = Math.min(getY(), y), lowZ = Math.min(getZ(), z);
        return Math.sqrt(Math.pow((highX - lowX), 2) + (checkYAxis ? Math.pow(highY - lowY, 2) : 0) + Math.pow((highZ - lowZ), 2));
    }

    @Override
    public String toString() {
        return (getWorldName().replace("\"", "\\\"").replace("'", "\\'")
                + "," + getX() + "," + getY() + "," + getZ() + "," + getYaw() + "," + getPitch());
    }

    public Location asBukkitLocation() {
        return new Location(RoblePlugin.INSTANCE.getServer().getWorld(getWorldName()), getX(), getY(), getZ(), getYaw(), getPitch());
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(@NotNull String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

}