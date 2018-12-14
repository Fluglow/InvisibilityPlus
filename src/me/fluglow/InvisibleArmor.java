package me.fluglow;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

public class InvisibleArmor extends JavaPlugin {

	@Override
	public void onEnable() {
		saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		HideSettings userSettings = loadSettings();
		ProtocolLibrary.getProtocolManager().addPacketListener(new EquipmentPacketListener(this, ListenerPriority.NORMAL, userSettings, PacketType.Play.Server.ENTITY_EQUIPMENT)); //Create and register our equipment packet listener

		InvisibilityEffectListener effectListener = new InvisibilityEffectListener(this, userSettings);
		getServer().getPluginManager().registerEvents(effectListener, this); //Register potion effect listener

		//Hide armor of any invisible online players.
		for(Player p : getServer().getOnlinePlayers())
		{
			if(!p.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
			int duration = p.getPotionEffect(PotionEffectType.INVISIBILITY).getDuration();
			effectListener.fakeRemoveArmor(p, duration);
		}
	}

	private HideSettings loadSettings()
	{
		boolean hideHelmet = getConfig().getBoolean("hide_helmet");
		boolean hideChestplate = getConfig().getBoolean("hide_chestplate");
		boolean hideLeggings = getConfig().getBoolean("hide_leggings");
		boolean hideBoots = getConfig().getBoolean("hide_boots");
		boolean hideMainhand = getConfig().getBoolean("hide_item_in_main_hand");
		boolean hideOffhand = getConfig().getBoolean("hide_item_in_off_hand");
		return new HideSettings(hideHelmet, hideChestplate, hideLeggings, hideBoots, hideMainhand, hideOffhand);
	}
}
