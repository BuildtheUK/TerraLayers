package org.btuk.terralayers.datapack.compatability;

public class PackVersion {
    public static String getPackVersion(String minecraftVersion) {

        if (minecraftVersion == null) {
            throw new IllegalArgumentException("Minecraft version cannot be null");
        }

        String[] versionParts = minecraftVersion.split("\\.");

        if (versionParts.length < 2 || versionParts.length > 3) {
            throw new IllegalArgumentException("Invalid Minecraft version: " + minecraftVersion);
        }

        return switch(minecraftVersion) {
            case "1.21.11" -> "94.1"; // 1.21.11 is the first supported version.
            default -> throw new IllegalArgumentException("Unsupported Minecraft version: " + minecraftVersion);
        };
    }
}
