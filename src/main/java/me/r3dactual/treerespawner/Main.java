package me.r3dactual.treerespawner;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

// TODO: Add Config for GROW_DELAY, CHECK_INTERVAL, etc.

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
    int checkRgInterval;
    int checkInterval;
    int growDelay;

    @Override
    public void onEnable() {
        // Load config.yml file and get necessary values
        saveDefaultConfig();
        checkRgInterval = getConfig().getInt("check-rg-interval");
        checkInterval = getConfig().getInt("check-interval");
        growDelay = getConfig().getInt("grow-delay");

        findNewRegions();
        treeRespawner();
    }

    @Override
    public void onDisable() {
        // Save the configuration file
        saveConfig();
    }

    public void getTreeMineRegions(){
        // Get the WorldGuard plugin instance
        WorldGuard worldGuard = WorldGuard.getInstance();

        // Iterate through all worlds
        for (World world : Bukkit.getWorlds()) {
            // Get the regions for the world
            Map<String, ProtectedRegion> regions = worldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)).getRegions();

            // Create a new folder for the regions
            File treeMinesFolder = new File(getDataFolder(), "treemines/" + world.getName());
            treeMinesFolder.mkdirs();

            // Iterate through the regions
            for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet()) {
                String regionName = entry.getKey();

                // Check if the region name starts with "tr_" or "treerespawn_"
                if (regionName.startsWith("tr_") || regionName.startsWith("treerespawn_")) {
                    // Create a new .yml file for the region
                    File regionFile = new File(treeMinesFolder, regionName + ".yml");

                    // Check if a file with the same name already exists in the TreeMines folder
                    if (regionFile.exists()) {
                        // The file already exists, so we don't need to do anything
                        return;
                    }

                    // The file does not exist, so we can create it
                    try {
                        regionFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void growSaplingsInRegion(World world, ProtectedRegion region) {
        // Get the bounding box for the region
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        // Iterate over all blocks within the bounding box
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    // Check if the block at the location is dirt
                    if (world.getBlockAt(x, y, z).getType() == Material.DIRT) {
                        // Check if there is already a tree or sapling at the location
                        if (world.getBlockAt(x, y + 1, z).getType() != Material.OAK_SAPLING && world.getBlockAt(x, y + 1, z).getType() != Material.OAK_LOG) {
                            int finalX = x;
                            int finalY = y + 1;
                            int finalZ = z;

                            // Place a sapling on top of the dirt block
                            world.getBlockAt(finalX, finalY, finalZ).setType(Material.OAK_SAPLING);
                            getLogger().info("Placed sapling at " + finalX + ", " + finalY + ", " + finalZ);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    // Remove sapling then after generate tree
                                    world.getBlockAt(finalX, finalY, finalZ).setType(Material.AIR);
                                    // Grow the sapling into a tree
                                    if(world.generateTree(new Location(world, finalX, finalY, finalZ), TreeType.TREE)){
                                        getLogger().info("Grew tree at " + finalX + ", " + finalY + ", " + finalZ);
                                    }else{
                                        getLogger().info("Couldn't Grew tree at " + finalX + ", " + finalY + ", " + finalZ);
                                    }
                                }
                            }.runTaskLater(this, growDelay * 20);
                        }
                    }
                }
            }
        }
    }

    public void treeRespawner() {
        // Start the tree respawner task
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    // Get the region manager for the world
                    RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                    if (regionManager == null) {
                        return;
                    }

                    Map<String, ProtectedRegion> regions = regionManager.getRegions();
                    // Iterate through the regions
                    for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet()) {
                        String regionName = entry.getKey();

                        // Get the region name with regionName
                        ProtectedRegion region = regionManager.getRegion(regionName);
                        if (region == null) {
                            return;
                        }

                        // Check if regions contains tr_ or treerespawn_
                        if (regionName.startsWith("tr_") || regionName.startsWith("treerespawn_")) {
                            // Grow a sapling at the random location
                            growSaplingsInRegion(world, region);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, checkInterval * 20);
    }

    public void findNewRegions(){
        // Start a new Bukkit runnable that will check for new regions every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                getTreeMineRegions();
            }
        }.runTaskTimer(this, 0, checkRgInterval * 20); // Run the task every 20 ticks (1 tick = 1/20 seconds), which is equivalent to 1 minute
    }
}
