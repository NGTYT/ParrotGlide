package me.workagum;

import static me.workagum.Plugin.inst;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class Utils {
    static Parrot carry(Player p) {
        return (p.isOnline() && p.getPassengers().size() > 0 && p.getPassengers().get(0) instanceof Parrot)
        /* @formatter:off */ ? (Parrot) p.getPassengers().get(0) : null; // @formatter:on
    }

    @SuppressWarnings("deprecation")
    static boolean isHeadItem(ItemStack item) {
        if (item == null || item.getType() != inst.conf.ITEM_TYPE) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getDisplayName().equals(inst.conf.HEAD_DNAME) || !meta.hasEnchant(inst.conf.ench)) return false;
        return !(meta instanceof SkullMeta) || ((SkullMeta) meta).getOwner().equals(inst.conf.HEAD_PNAME);
    }

    static void launch(Player player, Entity Parrot) {
        player.eject();
        double velocity = velocity();
        if (velocity > 0) Parrot.setVelocity(player.getLocation().getDirection().normalize().multiply(velocity));
    }

    static void taskStart() {
        if (inst.task == null || inst.task.isCancelled()) inst.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (isCancelled()) return;
                if (inst.gliders.size() == 0) {
                    cancel();
                    return;
                }

                for (UUID uuid : inst.gliders) {
                    Player  player  = Bukkit.getPlayer(uuid);
                    Parrot Parrot = carry(player);
                    if (Parrot != null) Parrot.damage(inst.conf.DAMAGES_AMOUNT, player);
                }
            }
        }.runTaskTimer(inst, 1, inst.conf.DAMAGES_FREQ * 20);
    }

    static void taskStop() {
        if (inst.task != null && !inst.task.isCancelled()) inst.task.cancel();
        inst.task = null;
    }

    static double velocity() {
        return (inst.conf.VEL_MIN == inst.conf.VEL_MAX) ? inst.conf.VEL_MIN
                : ((inst.conf.VEL_MAX - inst.conf.VEL_MIN) * Math.random()) + inst.conf.VEL_MIN;
    }
}