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

    // The delay (in ticks) before checking to place saplings
    private static final int CHECK_INTERVAL = 20 * 30;  // Check every 30 seconds

    // The delay (in ticks) before a placed sapling grows into a tree
    private static final int GROW_DELAY = 20 * 5;  // 5 seconds

    // The WorldGuard region where trees should respawn
    private static final String REGION_NAME = "tree-respawn-area";

    public void start() {
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

                    // Get the region with the name REGION_NAME
                    ProtectedRegion region = regionManager.getRegion(REGION_NAME);
                    if (region == null) {
                        return;
                    }

                    // Grow a sapling at the random location
                    growSaplingsInRegion(world, region);
                }
            }
        }.runTaskTimer(this, 0, CHECK_INTERVAL);
    }

    @Override
    public void onEnable() {
        start();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
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
                            }.runTaskLater(this, GROW_DELAY);
                        }
                    }
                }
            }
        }
    }
}
