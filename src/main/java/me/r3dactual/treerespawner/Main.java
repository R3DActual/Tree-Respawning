package me.r3dactual.treerespawner;

import me.r3dactual.treerespawner.managers.TreeMineManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

// TODO: Redo growDelay where it reads growDelay from each Mines config file

// TODO: Redo how the REGION_NAME works
//  for example check all regions within WorldGuard
//  and if they start with "tr_" or "treerespawner_"
//  they save within the TreeRespawner folder and have
//  values of what types of trees will grow within that region

// TODO: Maybe... add a function so if the tree is being cut
//  from the bottom it takes some time to cut that tree
//  to simulate cutting down an entire tree. Then once
//  tree is cut down remove the leaves

public final class Main extends JavaPlugin implements Listener {
    private static Main instance;

    public int checkRgInterval;

    @Override
    public void onEnable() {
        instance = this;
        // Load config.yml file and get necessary values
        saveDefaultConfig();
        reloadConfig();

        checkRgInterval = getConfig().getInt("check-rg-interval");

        TreeMineManager.findNewRegions();
        TreeMineManager.treeRespawner();
    }

    @Override
    public void onDisable() {
        // Save the configuration file
        saveConfig();
    }

    public static Main getInstance() {
        return instance;
    }
}
