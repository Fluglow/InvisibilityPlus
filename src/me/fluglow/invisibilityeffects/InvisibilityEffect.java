package me.fluglow.invisibilityeffects;

import me.fluglow.HideSettings;

import java.util.UUID;

public interface InvisibilityEffect {
	HideSettings.HideSetting[] getHidingCases();
	void onPlayerHide(UUID uuid);
	void onPlayerReveal(UUID uuid);
}
