package me.fluglow;

import com.comphenix.protocol.wrappers.EnumWrappers;

public class HideSettings {
	private final boolean hideHelmet;
	private final boolean hideChestplate;
	private final boolean hideLeggings;
	private final boolean hideBoots;
	private final boolean hideMainhand;
	private final boolean hideOffhand;

	HideSettings(boolean hideHelmet, boolean hideChestplate, boolean hideLeggings, boolean hideBoots, boolean hideMainhand, boolean hideOffhand)
	{
		this.hideHelmet = hideHelmet;
		this.hideChestplate = hideChestplate;
		this.hideLeggings = hideLeggings;
		this.hideBoots = hideBoots;
		this.hideMainhand = hideMainhand;
		this.hideOffhand = hideOffhand;
	}

	boolean shouldHide(EnumWrappers.ItemSlot slot)
	{
		boolean shouldHide = false;
		switch(slot) {
			case MAINHAND:
				shouldHide = hideMainhand;
				break;
			case OFFHAND:
				shouldHide = hideOffhand;
				break;
			case FEET:
				shouldHide = hideBoots;
				break;
			case LEGS:
				shouldHide = hideLeggings;
				break;
			case CHEST:
				shouldHide = hideChestplate;
				break;
			case HEAD:
				shouldHide = hideHelmet;
				break;
		}
		return shouldHide;
	}
}
