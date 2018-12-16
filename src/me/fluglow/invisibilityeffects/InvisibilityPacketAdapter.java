package me.fluglow.invisibilityeffects;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import org.bukkit.plugin.Plugin;

public abstract class InvisibilityPacketAdapter extends PacketAdapter implements InvisibilityEffect {

	InvisibilityPacketAdapter(Plugin plugin, PacketType... types) {
		super(plugin, types);
	}
}
