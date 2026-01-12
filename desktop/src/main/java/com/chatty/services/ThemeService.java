package com.chatty.services;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.util.prefs.Preferences;

public class ThemeService {
    private static final String THEME_PREF_KEY = "app.theme";
    private static final String DEFAULT_THEME = "light";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeService.class);

    // Biến theo dõi theme hiện tại
    private static final ObjectProperty<Theme> currentTheme = new SimpleObjectProperty<>(loadThemeFromPrefs());

    public enum Theme {
        // Khai báo đường dẫn CSS ngay trong Enum
        LIGHT("light", "/styles.css"),
        DARK("dark", "/styles-dark.css");

        private final String value;
        private final String cssPath;

        Theme(String value, String cssPath) {
            this.value = value;
            this.cssPath = cssPath;
        }

        public String getValue() { return value; }
        public String getCssPath() { return cssPath; }

        public static Theme fromString(String value) {
            for (Theme theme : Theme.values()) {
                if (theme.value.equals(value)) return theme;
            }
            return LIGHT;
        }
    }

    private static Theme loadThemeFromPrefs() {
        String themeValue = prefs.get(THEME_PREF_KEY, DEFAULT_THEME);
        return Theme.fromString(themeValue);
    }

    public static void setTheme(Theme theme) {
        currentTheme.set(theme);
        prefs.put(THEME_PREF_KEY, theme.getValue());
    }

    public static Theme getTheme() {
        return currentTheme.get();
    }

    // --- ĐÂY LÀ HÀM BẠN CẦN GIỮ LẠI ---
    // Hàm này lấy đường dẫn CSS của theme hiện tại
    public static String getThemeStylesheet() {
        // Lấy đường dẫn từ enum của theme đang chọn
        return currentTheme.get().getCssPath();
    }
    // ----------------------------------

    // Hàm tự động cài đặt và lắng nghe thay đổi theme cho Scene
    public static void install(Scene scene) {
        if (scene == null) return;

        applyCss(scene, currentTheme.get());

        currentTheme.addListener((obs, oldTheme, newTheme) -> {
            Platform.runLater(() -> applyCss(scene, newTheme));
        });
    }

    private static void applyCss(Scene scene, Theme theme) {
        scene.getStylesheets().clear();
        String cssUrl = ThemeService.class.getResource(theme.getCssPath()).toExternalForm();
        scene.getStylesheets().add(cssUrl);
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null) return;

        DialogPane dialogPane = dialog.getDialogPane();

        // Xóa style cũ
        dialogPane.getStylesheets().clear();

        // Thêm style theme hiện tại
        dialogPane.getStylesheets().add(getThemeStylesheet());

        // Thêm class để CSS nhận diện đây là dialog
        dialogPane.getStyleClass().add("custom-dialog");
    }
}