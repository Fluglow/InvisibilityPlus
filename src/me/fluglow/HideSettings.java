package me.fluglow;

import com.comphenix.protocol.wrappers.EnumWrappers;

public class HideSettings {

	private final boolean[] settings;
	HideSettings(boolean... settings)
	{
		this.settings = settings;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean shouldHideSlot(EnumWrappers.ItemSlot slot)
	{
		switch(slot) {
			case MAINHAND:
				return shouldHide(HideSetting.ITEM_IN_MAIN_HAND);
			case OFFHAND:
				return shouldHide(HideSetting.ITEM_IN_OFF_HAND);
			case FEET:
				return shouldHide(HideSetting.BOOTS);
			case LEGS:
				return shouldHide(HideSetting.LEGGINGS);
			case CHEST:
				return shouldHide(HideSetting.CHESTPLATE);
			case HEAD:
				return shouldHide(HideSetting.HELMET);
			default:
				return false;
		}
	}

	public boolean shouldHide(HideSetting setting)
	{
		return settings[setting.index];
	}


	public enum HideSetting { //Names are used in config, should be user-friendly.
		HELMET(0, true),
		CHESTPLATE(1, true),
		LEGGINGS(2, true),
		BOOTS(3, true),
		ITEM_IN_MAIN_HAND(4, false),
		ITEM_IN_OFF_HAND(5, false),
		BODY_ARROWS(6, false),
		SHOT_ARROWS(7, false),
		POTION_PARTICLES(8, false);

		private final int index;
		final boolean defValue;
		HideSetting(int index, boolean defaultValue)
		{
			this.index = index;
			this.defValue = defaultValue;
		}

	}
}
