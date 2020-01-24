package me.fluglow.invisibilityeffects;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.primitives.Doubles;
import me.fluglow.HideSettings;
import me.fluglow.InvisibilityPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SpawnedObjectHider extends InvisibilityPacketAdapter implements Listener {

	private Map<UUID, List<Entity>> spawnedEntities = new HashMap<>();
	private final HideSettings hideSettings;

	public SpawnedObjectHider(Plugin plugin, HideSettings hideSettings) {
		super(plugin, PacketType.Play.Server.SPAWN_ENTITY);
		this.hideSettings = hideSettings;
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public HideSettings.HideSetting[] getHidingCases() {
		return new HideSettings.HideSetting[] {
				HideSettings.HideSetting.SHOT_ARROWS
		};
	}

	@Override
	public void onPlayerHide(UUID uuid) {

	}

	@Override
	public void onPlayerReveal(UUID uuid) {
		if(!spawnedEntities.containsKey(uuid)) return;
		for(Entity e : spawnedEntities.get(uuid))
		{
			if(!e.isValid()) continue; //Picked up arrows are not valid entities
			for(Player p : e.getWorld().getPlayers())
			{
				if(p.getUniqueId().equals(uuid)) continue;
				try {
					PacketContainer entityPacket = createObjectPacket(e);
					ProtocolLibrary.getProtocolManager().sendServerPacket(p, entityPacket, false);
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
			}
		}
		spawnedEntities.remove(uuid);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event)
	{
		onPlayerReveal(event.getPlayer().getUniqueId());
	}

	@SuppressWarnings("UnstableApiUsage")
	private PacketContainer createObjectPacket(Entity e)
	{
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
		packet.getIntegers().write(0, e.getEntityId());
		packet.getUUIDs().write(0, e.getUniqueId());
		packet.getDoubles().write(0, e.getLocation().getX());
		packet.getDoubles().write(1, e.getLocation().getY());
		packet.getDoubles().write(2, e.getLocation().getZ());

		Vector velocity = e.getVelocity();

		int velX = (int)(Doubles.constrainToRange(velocity.getX(), -3.9D, 3.9D) * 8000.0D);
		int velY = (int)(Doubles.constrainToRange(velocity.getY(), -3.9D, 3.9D) * 8000.0D);
		int velZ = (int)(Doubles.constrainToRange(velocity.getZ(), -3.9D, 3.9D) * 8000.0D);

		packet.getIntegers().write(1, velX);
		packet.getIntegers().write(2, velY);
		packet.getIntegers().write(3, velZ);

		packet.getIntegers().write(4, (int)Math.floor(e.getLocation().getYaw() * 256.0F / 360.0F));
		packet.getIntegers().write(5, (int)Math.floor(e.getLocation().getPitch() * 256.0F / 360.0F));
		packet.getIntegers().write(6, getObjectIdFor(e));
		packet.getIntegers().write(7, getObjectDataFor(e));
		return packet;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if(event.getPacketType() != PacketType.Play.Server.SPAWN_ENTITY) return;
		Player player = event.getPlayer();

		PacketContainer packet = event.getPacket();
		UUID entityUUID = packet.getUUIDs().getValues().get(0);
		//We need to go through the entity modifier instead of calling Bukkit.getEntity(UUID) since it returns null.
		for(Entity entity : packet.getEntityModifier(player.getWorld()).getValues())
		{
			if(entity == null || !entity.getUniqueId().equals(entityUUID)) continue;

			if(!shouldHide(entity)) return; //Check settings

			Player spawner = getEntitySpawner(entity); //Get player who spawned this entity
			if(spawner == null || !InvisibilityPlus.isInvisible(spawner)) return; //If spawner doesn't exist or isn't invisible
			if(player.getUniqueId() == spawner.getUniqueId())
			{
				List<Entity> spawned = spawnedEntities.getOrDefault(spawner.getUniqueId(), new ArrayList<>());
				spawned.add(entity);
				spawnedEntities.put(spawner.getUniqueId(), spawned);
				return; //Don't hide player's entities from self.
			}

			event.setCancelled(true);
			return;

		}
	}

	private boolean shouldHide(Entity e)
	{
		if(e instanceof Arrow)
		{
			return hideSettings.shouldHide(HideSettings.HideSetting.SHOT_ARROWS);
		}
		return false;
	}

	private int getObjectDataFor(Entity e) //Some objects have object data that's required in the packet.
	{
		if(e instanceof Arrow)
		{
			Player shooter = getEntitySpawner(e);
			if(shooter == null) return 0;
			return shooter.getEntityId();
		}
		return 0;
	}

	private int getObjectIdFor(Entity e) //Object ids are hard coded in EntityTrackerEntry according to decompiler, so we'll just add them here when support is added for other object types.
	{
		if(e instanceof Arrow)
		{
			return 60;
		}
		return -1;
	}

	//Returns player who spawned the entity or null.
	private Player getEntitySpawner(Entity e)
	{
		if(e instanceof Arrow) //Hide arrow if shooter is invisible
		{
			Arrow arrow = (Arrow)e;
			ProjectileSource source = arrow.getShooter();
			if(!(source instanceof Player)) return null;

			return (Player)source;
		}
		return null;
	}
}
