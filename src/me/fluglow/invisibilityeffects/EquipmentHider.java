package me.fluglow.invisibilityeffects;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.fluglow.HideSettings;
import me.fluglow.InvisibilityPlus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class EquipmentHider extends InvisibilityPacketAdapter {

	private final HideSettings hideSettings;

	public EquipmentHider(Plugin plugin, HideSettings hideSettings) {
		super(plugin, PacketType.Play.Server.ENTITY_EQUIPMENT);
		this.hideSettings = hideSettings;
	}

	@Override
	public HideSettings.HideSetting[] getHidingCases() {
		return new HideSettings.HideSetting[] {
				HideSettings.HideSetting.HELMET,
				HideSettings.HideSetting.CHESTPLATE,
				HideSettings.HideSetting.LEGGINGS,
				HideSettings.HideSetting.BOOTS,
				HideSettings.HideSetting.ITEM_IN_MAIN_HAND,
				HideSettings.HideSetting.ITEM_IN_OFF_HAND
		};
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) return;

		PacketContainer packet = event.getPacket();
		Player player = event.getPlayer();
		int entityId = packet.getIntegers().getValues().get(0);
		for(Player onlinePlayer : player.getWorld().getPlayers())
		{
			if(onlinePlayer.getEntityId() == entityId && InvisibilityPlus.isInvisible(onlinePlayer))
			{
				EnumWrappers.ItemSlot slot = packet.getItemSlots().getValues().get(0);
				if(!hideSettings.shouldHideSlot(slot)) continue;
				packet.getItemModifier().write(0, null);
			}
		}
	}

	@Override
	public void onPlayerHide(UUID uuid) {
		fakeRemoveArmor(uuid);
	}

	@Override
	public void onPlayerReveal(UUID uuid) {
		sendActualArmor(uuid);
	}

	private void sendActualArmor(UUID playerUUID) //Sends the actual armor of a player to players in the same world
	{
		Player player;
		if((player = Bukkit.getPlayer(playerUUID)) == null) return; //If the player leaves during the invisibility cooldown, return.

		for(EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values())
		{
			if(!hideSettings.shouldHideSlot(slot)) continue;
			PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
			packetContainer.getIntegers().write(0, player.getEntityId());
			packetContainer.getItemSlots().write(0, slot);
			packetContainer.getItemModifier().write(0, getItemInSlot(player, slot));
			for(Player onlinePlayer : player.getWorld().getPlayers())
			{
				if(onlinePlayer.getUniqueId().equals(player.getUniqueId())) continue;

				try {
					ProtocolLibrary.getProtocolManager().sendServerPacket(onlinePlayer, packetContainer, false); //false=ignore listeners that don't just monitor
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void fakeRemoveArmor(UUID uuid) //Sends other players empty equipment packets of the specified player
	{
		Player player = Bukkit.getPlayer(uuid);
		if(player == null) return;
		for(EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values())
		{
			if(!hideSettings.shouldHideSlot(slot)) continue;
			PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
			packetContainer.getIntegers().write(0, player.getEntityId());
			packetContainer.getItemSlots().write(0, slot);
			for(Player onlinePlayer : player.getWorld().getPlayers())
			{
				if(onlinePlayer.getUniqueId().equals(uuid)) continue;
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
}
