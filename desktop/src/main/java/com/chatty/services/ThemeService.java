package com.chatty.services;

import java.util.prefs.Preferences;

public class ThemeService {
    private static final String THEME_PREF_KEY = "app.theme";
    private static final String DEFAULT_THEME = "light";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeService.class);

    public enum Theme {
        LIGHT("light"),
        DARK("dark");

        private final String value;

        Theme(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Theme fromString(String value) {
            for (Theme theme : Theme.values()) {
                if (theme.value.equals(value)) {
                    return theme;
                }
            }
            return LIGHT;
        }
    }

    public static void setTheme(Theme theme) {
        prefs.put(THEME_PREF_KEY, theme.getValue());
    }

    public static Theme getTheme() {
        String themeValue = prefs.get(THEME_PREF_KEY, DEFAULT_THEME);
        return Theme.fromString(themeValue);
    }

    public static String getThemeStylesheet() {
        Theme theme = getTheme();
        return theme == Theme.DARK ? "/styles-dark.css" : "/styles.css";
    }
}

