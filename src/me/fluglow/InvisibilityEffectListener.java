package me.fluglow;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class InvisibilityEffectListener implements Listener {

	private final InvisibleArmor mainPlugin;
	private final HideSettings hideSettings;

	private Map<UUID, BukkitTask> armorResetTasks = new HashMap<>();

	InvisibilityEffectListener(InvisibleArmor mainPlugin, HideSettings hideSettings)
	{
		this.mainPlugin = mainPlugin;
		this.hideSettings = hideSettings;
	}

	private void sendActualArmor(UUID playerUUID) //Sends the actual armor of a player to players in the same world
	{
		armorResetTasks.remove(playerUUID);
		Player player;
		if((player = Bukkit.getPlayer(playerUUID)) == null) return; //If the player leaves during the invisibility cooldown, return.

		for(EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values())
		{
			if(!hideSettings.shouldHide(slot)) continue;
			PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
			packetContainer.getIntegers().write(0, player.getEntityId());
			packetContainer.getItemSlots().write(0, slot);
			packetContainer.getItemModifier().write(0, getItemInSlot(player, slot));
			for(Player onlinePlayer : player.getWorld().getPlayers())
			{
				if(onlinePlayer.getUniqueId().equals(player.getUniqueId())) continue;

				try {
					ProtocolLibrary.getProtocolManager().sendServerPacket(onlinePlayer, packetContainer);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private ItemStack getItemInSlot(Player player, EnumWrappers.ItemSlot slot) //Returns an ItemStack from the players inventory in the specified slot
	{
		PlayerInventory inv = player.getInventory();
		ItemStack item = null;
		switch (slot) {
			case MAINHAND:
				item = inv.getItemInMainHand();
				break;
			case OFFHAND:
				item = inv.getItemInOffHand();
				break;
			case FEET:
				item = inv.getBoots();
				break;
			case LEGS:
				item = inv.getLeggings();
				break;
			case CHEST:
				item = inv.getChestplate();
				break;
			case HEAD:
				item = inv.getHelmet();
				break;
		}

		//Sending equipment packet with AIR crashes the client and prints a Netty NPE.
		return item == null || item.getType() == Material.AIR ? null : item;
	}

	private void fakeRemoveArmor(Player player, long potionDurationTicks) //Sends other players empty equipment packets of the specified player
	{
		UUID playerUUID = player.getUniqueId();
		for(EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values())
		{
			if(!hideSettings.shouldHide(slot)) continue;
			PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
			packetContainer.getIntegers().write(0, player.getEntityId());
			packetContainer.getItemSlots().write(0, slot);
			for(Player onlinePlayer : player.getWorld().getPlayers())
			{
				if(onlinePlayer.getUniqueId().equals(playerUUID)) continue;
				try {
					ProtocolLibrary.getProtocolManager().sendServerPacket(onlinePlayer, packetContainer);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}


		BukkitTask task = new BukkitRunnable() {
			@Override
			public void run() {
				sendActualArmor(playerUUID);
			}
		}.runTaskLater(mainPlugin, potionDurationTicks + 1L); //One second delay so our actual armor packets don't get set to null by our listener because the player is still invisible.
		if(armorResetTasks.containsKey(playerUUID))
		{
			armorResetTasks.get(playerUUID).cancel();
		}
		armorResetTasks.put(playerUUID, task);
	}

	@EventHandler
	public void playerJoinInvisibility(PlayerJoinEvent event)
	{
		if(!event.getPlayer().hasPermission("invisiblearmor.invisible")) return;
		if(event.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY))
		{
			fakeRemoveArmor(event.getPlayer(), event.getPlayer().getPotionEffect(PotionEffectType.INVISIBILITY).getDuration());
		}
	}

	@EventHandler
	public void potionConsume(PlayerItemConsumeEvent event) //Called when player drinks a potion. Calls fakeRemoveArmor() if everything is valid.
	{
		if(!event.getPlayer().hasPermission("invisiblearmor.invisible")) return;

		ItemStack item = event.getItem();

		if(item.getType() == Material.MILK_BUCKET && event.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY))
		{
			new BukkitRunnable() {
				@Override
				public void run() {
					sendActualArmor(event.getPlayer().getUniqueId());
				}
			}.runTaskLater(mainPlugin, 1L);
			return;
		}

		if(!item.hasItemMeta()) return;
		if(!(item.getItemMeta() instanceof PotionMeta)) return;
		PotionMeta pMeta = (PotionMeta)item.getItemMeta();
		if(pMeta.getBasePotionData().getType() == PotionType.INVISIBILITY)
		{
			fakeRemoveArmor(event.getPlayer(), pMeta.getBasePotionData().isExtended() ? 8*60*20 : 3*60*20);
		}
	}

	@EventHandler
	public void potionSplash(PotionSplashEvent event) //Called when an entity is affected by a splash potion. Calls fakeRemoveArmor() if everything is valid.
	{
		List<Player> affected = new ArrayList<>();
		for(Entity entity : event.getAffectedEntities())
		{
			if(entity instanceof Player)
			{
				Player player = (Player)entity;
				if(!player.hasPermission("invisiblearmor.invisible")) return;
				affected.add(player);
			}
		}
		if(affected.isEmpty()) return;

		new BukkitRunnable() {
			@Override
			public void run() {
				for(Player p : affected)
				{
					if(!p.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
					fakeRemoveArmor(p, p.getPotionEffect(PotionEffectType.INVISIBILITY).getDuration());
				}
			}
		}.runTaskLater(mainPlugin, 1L);
	}
}
