package me.r3dactual.treerespawner.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.r3dactual.treerespawner.Main;
import me.r3dactual.treerespawner.utils.LogUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.Map;

// TODO: Fix so possibly not using bukkit runnable for treeSpawner????
// TODO: Maybe use a database system to store all mines in
// TODO: Redo getTreeMineRegions function

public class TreeMineManager {
    // Get the WorldGuard plugin instance
    static WorldGuard worldGuard = WorldGuard.getInstance();

    public static void getTreeMineRegions() {
        // Iterate through all worlds
        for (World world : Bukkit.getWorlds()) {
            // Get the regions for the world
            Map<String, ProtectedRegion> regions = worldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)).getRegions();

            // Create a new folder for the regions
            File treeMinesFolder = new File(Main.getInstance().getDataFolder(), "treemines/" + world.getName());
            if(!treeMinesFolder.exists()){
                treeMinesFolder.mkdirs();
            }

            // Iterate through the regions
            for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet()) {
                String regionName = entry.getKey();

                // Check if the region name starts with "tr_" or "treerespawn_"
                if (regionName.startsWith("tr_") || regionName.startsWith("treerespawn_")) {
                    // Create a new .yml file for the region
                    File regionFile = new File(treeMinesFolder, regionName + ".yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(regionFile);
                    // Check if a file with the same name already exists in the TreeMines folder
                    if (!regionFile.exists()) {
                        // File does not exist, so we can create it
                        try {
                            // Get the input stream for the .yml file in the resources folder
                            InputStream inputStream = Main.getInstance().getResource("treemine_temp.yml");
                            // Create a new output stream for the new file
                            OutputStream outputStream = new FileOutputStream(regionFile);

                            LogUtils.fine("Region " + regionName + " found & created config with default values.");
                            // Copy the data from the input stream to the output stream
                            int read;
                            byte[] bytes = new byte[1024];
                            while ((read = inputStream.read(bytes)) != -1) {
                                outputStream.write(bytes, 0, read);
                            }

                            // Close the input and output streams
                            inputStream.close();
                            outputStream.close();
                            // Load the data from the file into a FileConfiguration object
                            config.save(regionFile);
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static void growSaplingsInRegion(World world, ProtectedRegion region, int growDelay) {
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
                            LogUtils.info("Placed sapling at " + finalX + ", " + finalY + ", " + finalZ);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    // Remove sapling then after generate tree
                                    world.getBlockAt(finalX, finalY, finalZ).setType(Material.AIR);
                                    // Grow the sapling into a tree
                                    if(world.generateTree(new Location(world, finalX, finalY, finalZ), TreeType.TREE)){
                                        LogUtils.info("Grew tree at " + finalX + ", " + finalY + ", " + finalZ);
                                    }else{
                                        LogUtils.warning("Couldn't Grew tree at " + finalX + ", " + finalY + ", " + finalZ);
                                    }
                                }
                            }.runTaskLater(Main.getInstance(), growDelay * 20); // In seconds
                        }
                    }
                }
            }
        }
    }

    // New Tree Respawner Function
    public static void treeRespawner() {
        for (World world : Bukkit.getWorlds()) {
            // Get Tree Mine Folders
            File treeMinesFolder = new File(Main.getInstance().getDataFolder(), "treemines/" + world.getName());

            // Get the region manager for the world
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            // Get the regions for the world
            Map<String, ProtectedRegion> regions = regionManager.getRegions();

            // Iterate through the regions
            for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet()) {
                String regionName = entry.getKey();

                // Get the region name with regionName
                ProtectedRegion region = regionManager.getRegion(regionName);
                // Check if regions contains tr_ or treerespawn_
                if (regionName.startsWith("tr_") || regionName.startsWith("treerespawn_")) {
                    File treeMineFile = new File(treeMinesFolder, regionName + ".yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(treeMineFile);
                    int mineInterval = config.getInt("mineInterval");
                    int growDelay = config.getInt("growDelay");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Grow a sapling at the random location
                            growSaplingsInRegion(world, region, growDelay);
                        }
                    }.runTaskTimer(Main.getInstance(), 15 * 20,  mineInterval * 60 * 20); // In minutes

                }
            }
        }
    }

    // Old Tree Respawner Function
    public static void treeRespawner2() {
        // Start the tree respawner task
        /*
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
                            //growSaplingsInRegion(world, region);
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, Main.getInstance().checkInterval * 20);
        */
    }

    public static void findNewRegions(){
        // Start a new Bukkit runnable that will check for new regions every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                getTreeMineRegions();
            }
        }.runTaskTimer(Main.getInstance(), 10 * 20, Main.getInstance().checkRgInterval * 60 * 20); // Run the task every 20 ticks (1 tick = 1/20 seconds), which is equivalent to 1 minute
    }
}
