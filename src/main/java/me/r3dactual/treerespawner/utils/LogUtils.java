package me.r3dactual.treerespawner.utils;

import me.r3dactual.treerespawner.Main;

public class LogUtils {

    public static void severe(String message) {
        String coloredMessage = String.format("\u001B[31m%s\u001B[0m", message);
        Main.getInstance().getLogger().severe(coloredMessage);
    }

    public static void warning(String message) {
        String coloredMessage = String.format("\u001B[33m%s\u001B[0m", message);
        Main.getInstance().getLogger().warning(coloredMessage);
    }

    public static void info(String message) {
        String coloredMessage = String.format("\u001B[36m%s\u001B[0m", message);
        Main.getInstance().getLogger().info(coloredMessage);
    }

    public static void fine(String message) {
        String coloredMessage = String.format("\u001B[37m%s\u001B[0m", message);
        Main.getInstance().getLogger().fine(coloredMessage);
    }

}
