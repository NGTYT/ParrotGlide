package me.workagum;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityDismountEvent;

public class Listeners implements Listener {
  @EventHandler(ignoreCancelled = true)
  private void onBlockPlace(BlockPlaceEvent e) {
    if (Utils.isHeadItem(e.getItemInHand()))
      e.setCancelled(true); 
  }
  
  @EventHandler
  private void onChunkLoad(ChunkLoadEvent e) {
    if (EE.eeEnabled(e.getWorld()))
      EE.eeStart(e.getChunk()); 
  }
  
  @EventHandler
  private void onChunkUnload(ChunkUnloadEvent e) {
    if (EE.eeEnabled(e.getWorld()))
      EE.eeGiveUp(e.getChunk()); 
  }
  
  @EventHandler(ignoreCancelled = true)
  private void onEntityDismount(EntityDismountEvent e) {
    if (!(e.getEntity() instanceof Parrot) || !(e.getDismounted() instanceof Player))
      return; 
    Player player = (Player)e.getDismounted();
    if (Plugin.inst.conf.EJECT_STOP && Plugin.inst.conf.GLIDE) {
      player.removePotionEffect(PotionEffectType.SLOW_FALLING);
      if (Plugin.inst.conf.DAMAGES)
        Plugin.inst.gliders.remove(player.getUniqueId()); 
    } 
    PlayerInventory playerInventory = player.getInventory();
    for (int i = 0; i < playerInventory.getSize(); ) {
      if (Utils.isHeadItem(playerInventory.getItem(i)))
        playerInventory.setItem(i, null); 
      i++;
    } 
  }
  
  @EventHandler(ignoreCancelled = true)
  private void onPlayerInteract(PlayerInteractEvent e) {
    if (Plugin.inst.conf.GRASS || e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK)
      return; 
    Block b = e.getClickedBlock();
    if (b == null || (!b.getType().equals(Material.GRASS) && !b.getType().equals(Material.TALL_GRASS)))
      return; 
    Player player = e.getPlayer();
    if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR))
      return; 
    for (Entity entity : b.getWorld().getNearbyEntities(b.getLocation(), 1.0D, 1.0D, 1.0D)) {
      if (entity instanceof Parrot) {
        Bukkit.getPluginManager().callEvent((Event)new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND));
        break;
      } 
    } 
  }
  
  @EventHandler(ignoreCancelled = true)
  private void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
    if (e.getHand() == EquipmentSlot.OFF_HAND)
      return; 
    Entity entity = e.getRightClicked();
    if (!(entity instanceof Parrot) || (Plugin.inst.conf.NO_BABY && !((Ageable)entity).isAdult()))
      return; 
    Player p = e.getPlayer();
    if (p.getPassengers().size() > 0) {
      if (!Utils.carry(p).equals(entity) || !Utils.isHeadItem(p.getInventory().getItemInMainHand()))
        return; 
      if (Plugin.inst.conf.SNEAK_PLACE && p.isSneaking()) {
        p.eject();
        entity.teleport(p.getLocation());
        return;
      } 
      if (Plugin.inst.conf.LAUNCH_CLICK) {
        Utils.launch(p, entity);
      } else {
        p.eject();
      } 
      return;
    } 
    if (!p.hasPermission("cg.glide") || p.isInsideVehicle() || 
      !p.getInventory().getItemInMainHand().getType().equals(Material.AIR))
      return; 
    p.addPassenger(entity);
    if (Plugin.inst.conf.GLIDE)
      p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Plugin.inst.conf.MAX_DURATION * 20, 0, false, false, false)); 
    if (Plugin.inst.conf.DAMAGES) {
      Plugin.inst.gliders.add(p.getUniqueId());
      Utils.taskStart();
    } 
    p.getInventory().setItemInMainHand(Plugin.inst.conf.ITEM);
  }
  
  @EventHandler(ignoreCancelled = true)
  private void onPlayerItemHeld(PlayerItemHeldEvent e) {
    Player player = e.getPlayer();
    Parrot chicken = Utils.carry(player);
    if (chicken == null)
      return; 
    if (Plugin.inst.conf.LOCK_HAND) {
      e.setCancelled(true);
    } else if (Plugin.inst.conf.LAUNCH_SLOT) {
      Utils.launch(player, (Entity)chicken);
    } else {
      player.eject();
    } 
  }
  
  @EventHandler(ignoreCancelled = true)
  private void onInventoryClick(InventoryClickEvent e) {
    if (Utils.isHeadItem(e.getCurrentItem()))
      e.setCancelled(true); 
  }
  
  @EventHandler(ignoreCancelled = true)
  private void onPlayerDropItem(PlayerDropItemEvent e) {
    Item item = e.getItemDrop();
    if (!Utils.isHeadItem(item.getItemStack()))
      return; 
    item.remove();
    Player player = e.getPlayer();
    Parrot chicken = Utils.carry(player);
    if (chicken != null)
      Utils.launch(player, (Entity)chicken); 
  }
  
  @EventHandler(ignoreCancelled = true)
  private void onEntityPotionEffect(EntityPotionEffectEvent event) {
    if (!event.getCause().equals(EntityPotionEffectEvent.Cause.EXPIRATION) || (event
      .getAction() != EntityPotionEffectEvent.Action.REMOVED && event
      .getAction() != EntityPotionEffectEvent.Action.CLEARED))
      return; 
    Entity entity = event.getEntity();
    PotionEffectType type = event.getOldEffect().getType();
    if (Plugin.inst.conf.LEAVE && Plugin.inst.conf.GLIDE && entity instanceof Player && type
      .equals(PotionEffectType.SLOW_FALLING) && Utils.carry((Player)entity) != null)
      entity.eject(); 
    if (entity instanceof org.bukkit.entity.Cow && type.equals(EE.EEP) && entity.hasMetadata("gcEE"))
      EE.eeGiveUp(entity.getLocation().getChunk()); 
  }
}
