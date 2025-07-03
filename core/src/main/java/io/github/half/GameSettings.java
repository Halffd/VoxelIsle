package io.github.half;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameSettings {
    // Default settings
    private static final float DEFAULT_MOUSE_SENSITIVITY = 0.3f;
    private static final boolean DEFAULT_PLAYER_GRAVITY = true;

    // Preferences keys
    private static final String PREF_NAME = "VoxelGameSettings";
    private static final String KEY_MOUSE_SENSITIVITY = "mouseSensitivity";
    private static final String KEY_PLAYER_GRAVITY = "playerGravity";

    // Singleton instance
    private static GameSettings instance;

    // Settings values
    private float mouseSensitivity;
    private boolean playerGravityEnabled;
    private boolean wfcVerboseLoggingEnabled;

    // Preferences object
    private Preferences prefs;

    private GameSettings() {
        prefs = Gdx.app.getPreferences(PREF_NAME);
        loadSettings();
    }

    public static GameSettings getInstance() {
        if (instance == null) {
            instance = new GameSettings();
        }
        return instance;
    }

    private void loadSettings() {
        mouseSensitivity = prefs.getFloat(KEY_MOUSE_SENSITIVITY, DEFAULT_MOUSE_SENSITIVITY);
        playerGravityEnabled = prefs.getBoolean(KEY_PLAYER_GRAVITY, DEFAULT_PLAYER_GRAVITY);
    }

    public void saveSettings() {
        prefs.putFloat(KEY_MOUSE_SENSITIVITY, mouseSensitivity);
        prefs.putBoolean(KEY_PLAYER_GRAVITY, playerGravityEnabled);
        prefs.flush();
    }

    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = sensitivity;
    }

    public boolean isPlayerGravityEnabled() {
        return playerGravityEnabled;
    }

    public void setPlayerGravityEnabled(boolean enabled) {
        this.playerGravityEnabled = enabled;
    }

    public void togglePlayerGravity() {
        playerGravityEnabled = !playerGravityEnabled;
    }

    public boolean isWfcVerboseLoggingEnabled() {
        return wfcVerboseLoggingEnabled;
    }

    public void setWfcVerboseLoggingEnabled(boolean wfcVerboseLoggingEnabled) {
        this.wfcVerboseLoggingEnabled = wfcVerboseLoggingEnabled;
    }

    public void toggleWfcVerboseLogging() {
        this.wfcVerboseLoggingEnabled = !this.wfcVerboseLoggingEnabled;
    }
}
