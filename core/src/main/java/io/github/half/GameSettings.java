package io.github.half;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameSettings {
    // Default settings
    private static final float DEFAULT_MOUSE_SENSITIVITY = 0.3f;
    private static final boolean DEFAULT_PLAYER_GRAVITY = true;
    private static final boolean DEFAULT_COLLISION_LOGGING = true;
    private static final boolean DEFAULT_FULL_PREWARM = false;
    private static final boolean DEFAULT_WFC_VERBOSE_LOGGING = false;

    // Preferences keys
    private static final String PREF_NAME = "VoxelGameSettings";
    private static final String KEY_MOUSE_SENSITIVITY = "mouseSensitivity";
    private static final String KEY_PLAYER_GRAVITY = "playerGravity";
    private static final String KEY_COLLISION_LOGGING = "collisionLogging";
    private static final String KEY_FULL_PREWARM = "fullPrewarm";
    private static final String KEY_WFC_VERBOSE_LOGGING = "wfcVerbose";

    // Singleton instance
    private static GameSettings instance;

    // Settings values
    private float mouseSensitivity;
    private boolean playerGravityEnabled;
    private boolean wfcVerboseLoggingEnabled;
    private boolean collisionLoggingEnabled;
    private boolean fullPrewarmEnabled;

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
        collisionLoggingEnabled = prefs.getBoolean(KEY_COLLISION_LOGGING, DEFAULT_COLLISION_LOGGING);
        fullPrewarmEnabled = prefs.getBoolean(KEY_FULL_PREWARM, DEFAULT_FULL_PREWARM);
        wfcVerboseLoggingEnabled = prefs.getBoolean(KEY_WFC_VERBOSE_LOGGING, DEFAULT_WFC_VERBOSE_LOGGING);
    }

    public void saveSettings() {
        prefs.putFloat(KEY_MOUSE_SENSITIVITY, mouseSensitivity);
        prefs.putBoolean(KEY_PLAYER_GRAVITY, playerGravityEnabled);
        prefs.putBoolean(KEY_COLLISION_LOGGING, collisionLoggingEnabled);
        prefs.putBoolean(KEY_FULL_PREWARM, fullPrewarmEnabled);
        prefs.putBoolean(KEY_WFC_VERBOSE_LOGGING, wfcVerboseLoggingEnabled);
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

    // Collision logging
    public boolean isCollisionLoggingEnabled() {
        return collisionLoggingEnabled;
    }

    public void setCollisionLoggingEnabled(boolean enabled) {
        this.collisionLoggingEnabled = enabled;
    }

    public void toggleCollisionLogging() {
        this.collisionLoggingEnabled = !this.collisionLoggingEnabled;
    }

    // Full prewarm setting
    public boolean isFullPrewarmEnabled() {
        return fullPrewarmEnabled;
    }

    public void setFullPrewarmEnabled(boolean enabled) {
        this.fullPrewarmEnabled = enabled;
    }

    public void toggleFullPrewarm() {
        this.fullPrewarmEnabled = !this.fullPrewarmEnabled;
    }
}
