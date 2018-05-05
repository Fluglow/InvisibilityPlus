package me.fluglow;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import org.bukkit.plugin.java.JavaPlugin;

public class InvisibleArmor extends JavaPlugin {

	@Override
	public void onEnable() {
		saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		HideSettings userSettings = loadSettings();
		ProtocolLibrary.getProtocolManager().addPacketListener(new EquipmentPacketListener(this, ListenerPriority.NORMAL, userSettings, PacketType.Play.Server.ENTITY_EQUIPMENT)); //Create and register our equipment packet listener

		getServer().getPluginManager().registerEvents(new InvisibilityEffectListener(this, userSettings), this); //Register potion effect listener
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
