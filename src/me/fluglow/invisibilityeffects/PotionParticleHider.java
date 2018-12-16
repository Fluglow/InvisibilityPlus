package me.fluglow.invisibilityeffects;


import me.fluglow.HideSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class PotionParticleHider implements InvisibilityEffect {

	private final Plugin plugin;

	public PotionParticleHider(Plugin plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public void onPlayerHide(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		if(player == null) return;
		new BukkitRunnable() {
			@Override
			public void run() {
				PotionEffect currEffect = player.getPotionEffect(PotionEffectType.INVISIBILITY);
				if(currEffect == null) return;
				PotionEffect newEffect = new PotionEffect(PotionEffectType.INVISIBILITY, currEffect.getDuration(), currEffect.getAmplifier(), currEffect.isAmbient(), false);
				player.removePotionEffect(PotionEffectType.INVISIBILITY);
				player.addPotionEffect(newEffect);
			}
		}.runTaskLater(plugin, 1L);
	}

	@Override
	public void onPlayerReveal(UUID uuid) {

	}

	@Override
	public HideSettings.HideSetting[] getHidingCases() {
		return new HideSettings.HideSetting[] {
			HideSettings.HideSetting.POTION_PARTICLES
		};
	}
}
