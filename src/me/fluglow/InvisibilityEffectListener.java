package me.fluglow;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class InvisibilityEffectListener implements Listener {

	private final InvisibilityPlus mainPlugin;

	private Map<UUID, BukkitTask> revealTasks = new HashMap<>(); //Keeps track of tasks that reveal a player after an invisibility effect ends.

	InvisibilityEffectListener(InvisibilityPlus mainPlugin)
	{
		this.mainPlugin = mainPlugin;
	}

	private void revealPlayer(UUID player)
	{
		cancelResetTask(player);
		mainPlugin.setPlayerVisible(player, true);
	}

	void hidePlayer(UUID player, long reappearTicks)
	{
		mainPlugin.setPlayerVisible(player, false);

		BukkitTask task = new BukkitRunnable() { //Reveal player after reappearTicks
			@Override
			public void run() {
				revealPlayer(player);
			}
		}.runTaskLater(mainPlugin, reappearTicks);

		cancelResetTask(player);
		revealTasks.put(player, task);
	}

	private void cancelResetTask(UUID player)
	{
		if(!revealTasks.containsKey(player)) return;
		revealTasks.get(player).cancel();
		revealTasks.remove(player);
	}

	@EventHandler
	public void playerJoinInvisibility(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if(!InvisibilityPlus.hasPluginPermission(player)) return;
		if(player.hasPotionEffect(PotionEffectType.INVISIBILITY))
		{
			hidePlayer(player.getUniqueId(), player.getPotionEffect(PotionEffectType.INVISIBILITY).getDuration());
		}
	}

	@EventHandler
	public void potionConsume(PlayerItemConsumeEvent event) //Used to detect when a player drinks an invisibility potion or milk
	{
		Player player = event.getPlayer();
		if(!InvisibilityPlus.hasPluginPermission(player)) return;

		ItemStack item = event.getItem();

		if(item.getType() == Material.MILK_BUCKET && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) //Player drank milk, reveal player.
		{
			revealPlayer(player.getUniqueId());
			return;
		}

		if(!item.hasItemMeta()) return;
		if(!(item.getItemMeta() instanceof PotionMeta)) return;
		PotionMeta pMeta = (PotionMeta)item.getItemMeta();
		if(pMeta.getBasePotionData().getType() == PotionType.INVISIBILITY) //Player drank an invisibility potion, hide.
		{
			hidePlayer(player.getUniqueId(), pMeta.getBasePotionData().isExtended() ? 8*60*20 : 3*60*20);
		}
	}

	@EventHandler
	public void potionSplash(PotionSplashEvent event) //Called when an entity is affected by a splash potion. Calls fakeRemoveArmor() if everything is valid.
	{
		Collection<PotionEffect> effects = event.getPotion().getEffects();
		PotionEffect invisibilityEffect = null;
		for(PotionEffect effect : effects)
		{
			if(!effect.getType().equals(PotionEffectType.INVISIBILITY)) continue;
			invisibilityEffect = effect;
			break;
		}
		if(invisibilityEffect == null) return;

		List<Player> affected = new ArrayList<>();
		Map<UUID, Double> playerDurations = new HashMap<>();
		for(Entity entity : event.getAffectedEntities())
		{
			if(!(entity instanceof Player)) continue;

			Player player = (Player)entity;
			if(!InvisibilityPlus.hasPluginPermission(player)) continue;
			playerDurations.put(player.getUniqueId(), event.getIntensity(player) * invisibilityEffect.getDuration());

			PotionEffect currEffect = player.getPotionEffect(PotionEffectType.INVISIBILITY);
			if(currEffect != null)
			{
				long splashEffectTime = Math.round(playerDurations.get(player.getUniqueId()));
				if(splashEffectTime < currEffect.getDuration()) continue; //Skip players with a longer duration invisibility effect since it doesn't get overridden.
			}
			affected.add(player);

		}
		for(Player player : affected)
		{
			hidePlayer(player.getUniqueId(), Math.round(playerDurations.get(player.getUniqueId())));
		}
	}
}
