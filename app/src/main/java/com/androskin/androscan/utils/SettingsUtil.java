package com.androskin.androscan.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.androskin.androscan.enums.ExchangeMethods;

import java.util.Locale;

public class SettingsUtil {
    private static SettingsUtil instance;

    final String[] languages = {"English", "Українська"};

    private final SharedPreferences preferences;

    public static final String SETTINGS_FILE = "Androscan.properties";

    private final String PREFERENCES_LANGUAGE = "LANGUAGE";

    private final String PREFERENCES_DEVICE_ID = "DEVICE_ID";
    private final String PREFERENCES_USE_CAMERA = "USE_CAMERA";
    private final String PREFERENCES_WEIGHT_BARCODE_PREFIX = "WEIGHT_BARCODE_PREFIX";
    private final String PREFERENCES_CODE_LENGTH = "CODE_LENGTH";

    private final String PREFERENCES_EXCHANGE_METHOD = "EXCHANGE_METHOD";

    private final String PREFERENCES_LAN_DIRECTORY = "LAN_DIRECTORY";
    private final String PREFERENCES_LAN_USER = "LAN_USER";
    private final String PREFERENCES_LAN_PASSWORD = "LAN_PASSWORD";

    private final String PREFERENCES_FTP_ADDRESS = "FTP_SERVER_ADDRESS";
    private final String PREFERENCES_FTP_USER = "FTP_USER";
    private final String PREFERENCES_FTP_PASSWORD = "FTP_PASSWORD";
    private final String PREFERENCES_FTP_DIRECTORY = "FTP_DIRECTORY";
    private final String PREFERENCES_FTP_PASSIVE_MODE = "FTP_PASSIVE_MODE";

    private SettingsUtil(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public static synchronized SettingsUtil getInstance(SharedPreferences preferences) {
        if (instance == null) {
            instance = new SettingsUtil(preferences);
        }

        return instance;
    }

    public String[] getLanguagesList() {
        return languages;
    }

    public String getLanguage() {
        String result = "en";

        if (preferences.contains(PREFERENCES_LANGUAGE)) {
            result = preferences.getString(PREFERENCES_LANGUAGE, "en");
        }

        if (result.equals("")) result = "en";

        return result;
    }

    public void setLanguage(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_LANGUAGE, value);
        editor.apply();
    }

    public String getDeviceId() {
        if (preferences.contains(PREFERENCES_DEVICE_ID)) {
            return preferences.getString(PREFERENCES_DEVICE_ID, "");
        }

        return "";
    }

    public void setDeviceId(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_DEVICE_ID, value);
        editor.apply();
    }

    public boolean getUseCamera() {
        if (preferences.contains(PREFERENCES_USE_CAMERA)) {
            int result = preferences.getInt(PREFERENCES_USE_CAMERA, 0);
            return result == 1;
        }

        return false;
    }

    public void setUseCamera(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREFERENCES_USE_CAMERA, value?1:0);
        editor.apply();
    }

    public String getWeightBarcodePrefix() {
        if (preferences.contains(PREFERENCES_WEIGHT_BARCODE_PREFIX)) {
            return preferences.getString(PREFERENCES_WEIGHT_BARCODE_PREFIX, "");
        }

        return "";
    }

    public void setWeightBarcodePrefix(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_WEIGHT_BARCODE_PREFIX, value);
        editor.apply();
    }

    public String getCodeLength() {
        if (preferences.contains(PREFERENCES_CODE_LENGTH)) {
            return preferences.getString(PREFERENCES_CODE_LENGTH, "");
        }

        return "";
    }

    public void setCodeLength(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_CODE_LENGTH, value);
        editor.apply();
    }

    public ExchangeMethods getExchangeMethod() {
        int result = 0;

        if (preferences.contains(PREFERENCES_EXCHANGE_METHOD)) {
            result = preferences.getInt(PREFERENCES_EXCHANGE_METHOD, 0);
        }

        if (result == 1) {
            return ExchangeMethods.FTP;
        }

        return ExchangeMethods.LAN;
    }

    public void setExchangeMethod(ExchangeMethods value) {
        int result = 0;

        if (value == ExchangeMethods.FTP) {
            result = 1;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREFERENCES_EXCHANGE_METHOD, result);
        editor.apply();
    }

    public String getLanDirectory() {
        String result = "";

        if (preferences.contains(PREFERENCES_LAN_DIRECTORY)) {
            result = preferences.getString(PREFERENCES_LAN_DIRECTORY, "").trim();

            if (result.length() == 0) {
                return result;
            }

            result = result.replace('\\', '/');

            if (result.charAt(result.length() - 1) != '/') {
                result = result+"/";
            }
        }

        return result;
    }

    public void setLanDirectory(String value) {
        if (value == null) value = "";

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_LAN_DIRECTORY, value);
        editor.apply();
    }

    public String getLanUser() {
        if (preferences.contains(PREFERENCES_LAN_USER)) {
            return preferences.getString(PREFERENCES_LAN_USER, "");
        }

        return "";
    }

    public void setLanUser(String value) {
        if (value == null) value = "";

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_LAN_USER, value);
        editor.apply();
    }

    public String getLanPassword() {
        if (preferences.contains(PREFERENCES_LAN_PASSWORD)) {
            String password = preferences.getString(PREFERENCES_LAN_PASSWORD, "");
            if (!password.equals("")) {
                try {
                    password = AESCrypt.decrypt(password);
                } catch (Exception e) {
                    password = "";
                }
            }

            return password;
        }

        return "";
    }

    public void setLanPassword(String value) throws Exception {
        if (value == null) value = "";

        SharedPreferences.Editor editor = preferences.edit();
        String password = value;
        if (!password.equals("")) {
            password = AESCrypt.encrypt(password);
        }
        editor.putString(PREFERENCES_LAN_PASSWORD, password);
        editor.apply();
    }

    public String getFtpAddress() {
        if (preferences.contains(PREFERENCES_FTP_ADDRESS)) {
            return preferences.getString(PREFERENCES_FTP_ADDRESS, "");
        }

        return "";
    }

    public void setFtpAddress(String value) {
        if (value == null) value = "";

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_FTP_ADDRESS, value);
        editor.apply();
    }

    public String getFtpUser() {
        if (preferences.contains(PREFERENCES_FTP_USER)) {
            return preferences.getString(PREFERENCES_FTP_USER, "");
        }

        return "";
    }

    public void setFtpUser(String value) {
        if (value == null) value = "";

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_FTP_USER, value);
        editor.apply();
    }

    public String getFtpPassword() {
        if (preferences.contains(PREFERENCES_FTP_PASSWORD)) {
            String password = preferences.getString(PREFERENCES_FTP_PASSWORD, "");
            if (!password.equals("")) {
                try {
                    password = AESCrypt.decrypt(password);
                } catch (Exception e) {
                    password = "";
                }
            }

            return password;
        }

        return "";
    }

    public void setFtpPassword(String value) throws Exception {
        if (value == null) value = "";

        SharedPreferences.Editor editor = preferences.edit();
        String password = value;
        if (!password.equals("")) {
            password = AESCrypt.encrypt(password);
        }
        editor.putString(PREFERENCES_FTP_PASSWORD, password);
        editor.apply();
    }

    public String getFtpDirectory() {
        String result = "";

        if (preferences.contains(PREFERENCES_FTP_DIRECTORY)) {
            result = preferences.getString(PREFERENCES_FTP_DIRECTORY, "").trim();

            if (result.length() == 0) {
                return result;
            }

            result = result.replace('\\', '/');

            if (result.charAt(0) != '/') {
                result = "/"+result;
            }

            if (result.charAt(result.length() - 1) != '/') {
                result = result+"/";
            }
        }

        return result;
    }

    public void setFtpDirectory(String value) {
        if (value == null) value = "";

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCES_FTP_DIRECTORY, value);
        editor.apply();
    }

    public boolean getFtpPassiveMode() {
        if (preferences.contains(PREFERENCES_FTP_PASSIVE_MODE)) {
            int result = preferences.getInt(PREFERENCES_FTP_PASSIVE_MODE, 0);
            return result == 1;
        }

        return false;
    }

    public void setFtpPassiveMode(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREFERENCES_FTP_PASSIVE_MODE, value?1:0);
        editor.apply();
    }

    public String checkFtpSettings() {
        String result = "";

        final String host = getFtpAddress().trim();
        final String username = getFtpUser().trim();
        final String password = getFtpPassword().trim();

        if (host.length() < 1) {
            result = "Please Enter Server Address!";
        } else if (username.length() < 1) {
            result = "Please Enter User Name!";
        } else if (password.length() < 1) {
            result = "Please Enter Password!";
        }

        return result;
    }

    public void setLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale=locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

}
