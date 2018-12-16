package me.fluglow;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import me.fluglow.invisibilityeffects.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvisibilityPlus extends JavaPlugin {

	private List<InvisibilityEffect> invisibilityEffects = new ArrayList<>();

	@Override
	public void onEnable() {
		loadEffects(loadSettings());

		InvisibilityEffectListener effectListener = new InvisibilityEffectListener(this);
		getServer().getPluginManager().registerEvents(effectListener, this); //Register potion effect listener

		//Hide any invisible players
		for(Player p : getServer().getOnlinePlayers())
		{
			if(!p.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
			int duration = p.getPotionEffect(PotionEffectType.INVISIBILITY).getDuration();
			effectListener.hidePlayer(p.getUniqueId(), duration);
		}
	}

	private void loadEffects(HideSettings settings)
	{
		List<InvisibilityEffect> effects = new ArrayList<>();
		effects.add(new EquipmentHider(this, settings));
		effects.add(new SpawnedObjectHider(this, settings));
		effects.add(new BodyArrowHider(this));
		effects.add(new PotionParticleHider(this));

		for(InvisibilityEffect effect : effects)
		{
			if(!shouldEnable(effect, settings)) continue;
			invisibilityEffects.add(effect);
			if(!(effect instanceof PacketAdapter)) continue;
			ProtocolLibrary.getProtocolManager().addPacketListener((InvisibilityPacketAdapter)effect);
		}
	}

	private boolean shouldEnable(InvisibilityEffect effect, HideSettings currentSettings)
	{
		for(HideSettings.HideSetting setting : effect.getHidingCases())
		{
			if(currentSettings.shouldHide(setting)) return true;
		}
		return false;
	}

	void setPlayerVisible(UUID player, boolean visible)
	{
		for(InvisibilityEffect effect : invisibilityEffects)
		{
			if(visible) effect.onPlayerReveal(player);
			else effect.onPlayerHide(player);
		}
	}

	public static boolean isInvisible(Player player)
	{
		return (hasPluginPermission(player) && player.hasPotionEffect(PotionEffectType.INVISIBILITY));
	}

	static boolean hasPluginPermission(Player player)
	{
		//Checks both deprecated permission and current permission.
		return player.hasPermission("invisiblearmor.invisible") || player.hasPermission("invisibilityplus.invisible");
	}

	private HideSettings loadSettings()
	{
		HideSettings.HideSetting[] settings = HideSettings.HideSetting.values(); //Get all possible settings
		boolean[] booleans = new boolean[settings.length];

		for(int i = 0; i < settings.length; i++)
		{
			HideSettings.HideSetting setting = settings[i];
			String name = "hide_" + setting.name().toLowerCase(); //Create name for config key
			if(!getConfig().contains(name))
			{
				getConfig().set(name, setting.defValue); //Set value for non-existing key
			}

			booleans[i] = getConfig().getBoolean(name);
		}
		saveConfig();

		return new HideSettings(booleans);
	}
}
