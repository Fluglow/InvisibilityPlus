package me.fluglow.invisibilityeffects;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.fluglow.HideSettings;
import me.fluglow.InvisibilityPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class BodyArrowHider extends InvisibilityPacketAdapter implements Listener {

	private static final int DW_ARROW_INDEX = 10; //Arrow field index in a DataWatcher

	private Map<UUID, Integer> playerArrowAmounts = new HashMap<>();

	public BodyArrowHider(Plugin plugin) {
		super(plugin, PacketType.Play.Server.ENTITY_METADATA);
	}

	@Override
	public HideSettings.HideSetting[] getHidingCases() {
		return new HideSettings.HideSetting[] {
				HideSettings.HideSetting.BODY_ARROWS
		};
	}

	@Override
	public void onPlayerHide(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		if(player == null) return;
		if(getPlayerCurrentArrows(player) == 0) return;
		setPlayerFakeArrowCount(uuid, 0);
	}

	@Override
	public void onPlayerReveal(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		if(player == null) return;
		int current = getPlayerCurrentArrows(player);
		if(current == 0) return;
		setPlayerFakeArrowCount(uuid, current);
	}

	private int getPlayerCurrentArrows(Player player)
	{
		return playerArrowAmounts.getOrDefault(player.getUniqueId(), 0);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if(event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;
		PacketContainer packet = event.getPacket();

		int entityId = packet.getIntegers().getValues().get(0);

		Player recipient = event.getPlayer();
		Player dataOwner = null;
		for(Player p : recipient.getWorld().getPlayers())
		{
			if(p.getEntityId() != entityId) continue;
			dataOwner = p;
			break;
		}
		if(dataOwner == null) return; //Not a player

		List<WrappedWatchableObject> watchers = packet.getWatchableCollectionModifier().getValues().get(0);
		for(WrappedWatchableObject watcher : watchers)
		{
			if(watcher.getIndex() != DW_ARROW_INDEX) continue;
			playerArrowAmounts.put(dataOwner.getUniqueId(), (int)watcher.getValue());

			if(!InvisibilityPlus.isInvisible(dataOwner)) return;
			watcher.setValue(0, true);
		}
	}

	private void setPlayerFakeArrowCount(UUID uuid, int arrowCount)
	{
		Player player = Bukkit.getPlayer(uuid);
		if(player == null) return;
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		packet.getIntegers().write(0, player.getEntityId());

		WrappedDataWatcher watcher = new WrappedDataWatcher();
		watcher.setEntity(player);
		watcher.setObject(10, WrappedDataWatcher.Registry.get(Integer.class), arrowCount);
		packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
		for(Player wPlayer : player.getWorld().getPlayers())
		{
			try {
				ProtocolLibrary.getProtocolManager().sendServerPacket(wPlayer, packet, false); //false=ignore listeners that don't just monitor
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	@EventHandler
	public void playerQuit(PlayerQuitEvent event)
	{
		playerArrowAmounts.remove(event.getPlayer().getUniqueId());
	}
}
