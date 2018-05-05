package me.fluglow;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public class EquipmentPacketListener extends PacketAdapter {

	private final HideSettings hideSettings;

	EquipmentPacketListener(Plugin plugin, ListenerPriority listenerPriority, HideSettings hideSettings, PacketType... types) {
		super(plugin, listenerPriority, types);
		this.hideSettings = hideSettings;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if(event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT)
		{
			PacketContainer packet = event.getPacket();
			Player player = event.getPlayer();
			int entityId = packet.getIntegers().getValues().get(0);
			for(Player onlinePlayer : player.getWorld().getPlayers())
			{
				if(onlinePlayer.getEntityId() == entityId && onlinePlayer.hasPermission("invisiblearmor.invisible") && onlinePlayer.hasPotionEffect(PotionEffectType.INVISIBILITY))
				{
					EnumWrappers.ItemSlot slot = packet.getItemSlots().getValues().get(0);
					if(!hideSettings.shouldHide(slot)) continue;
					packet.getItemModifier().write(0, null);
				}
			}
		}
	}
}
