package com.androskin.androscan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentResultListener;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;

import com.androskin.androscan.dialogs.DialogMessage;
import com.androskin.androscan.dialogs.DialogYesNo;
import com.androskin.androscan.enums.EnumDialogValues;
import com.androskin.androscan.enums.ExchangeMethods;
import com.androskin.androscan.utils.ActivityUtil;
import com.androskin.androscan.utils.CameraUtil;
import com.androskin.androscan.utils.SettingsUtil;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = "GA_SettingsActivity";

    SettingsUtil settings;

    private Button backBtn;
    private Button btnSaveSettings, btnAbout;

    private Button btnChooseLanguage;

    TabHost tabHost;
    TabHost.TabSpec tabSpec;

    private EditText deviceId;
    private SwitchCompat useCamera;
    private EditText weightPrefix, codeLength;

    private ExchangeMethods exchangeMethod;
    private RadioButton radioButtonLan, radioButtonFtp;

    private RelativeLayout lanLayout, ftpLayout;

    private EditText lanDirectory, lanUser, lanPassword;

    private EditText ftpAddress, ftpUser, ftpPassword, ftpDirectory;
    private SwitchCompat ftpPassiveMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        settings = SettingsUtil.getInstance(MainActivity.sharedPreferences);
        settings.setLocale(getBaseContext(), settings.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActivityUtil.setPortraitOrientation(this);

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.SAVE_SETTINGS.toString(), this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String key, @NonNull Bundle bundle) {
                saveSettings();
            }
        });

        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(this);

        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(this);

        btnAbout = findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(this);


        btnChooseLanguage = findViewById(R.id.btnChooseLanguage);
        btnChooseLanguage.setOnClickListener(this);
        btnChooseLanguage.setText(settings.getLanguage());

        deviceId = findViewById(R.id.deviceId);
        useCamera = findViewById(R.id.useCamera);
        useCamera.setOnClickListener(this);
        weightPrefix = findViewById(R.id.weightPrefix);
        codeLength = findViewById(R.id.codeLength);


        radioButtonLan = findViewById(R.id.radioButtonLan);
        radioButtonLan.setOnClickListener(this);

        radioButtonFtp = findViewById(R.id.radioButtonFtp);
        radioButtonFtp.setOnClickListener(this);

        lanLayout = findViewById(R.id.lanLayout);
        ftpLayout = findViewById(R.id.ftpLayout);

        lanDirectory = findViewById(R.id.lanDirectory);
        lanUser = findViewById(R.id.lanUser);
        lanPassword = findViewById(R.id.lanPassword);

        ftpAddress = findViewById(R.id.ftpAddress);
        ftpUser = findViewById(R.id.ftpUser);
        ftpPassword = findViewById(R.id.ftpPassword);
        ftpDirectory = findViewById(R.id.ftpDirectory);
        ftpPassiveMode = findViewById(R.id.ftpPassiveMode);


        tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        tabSpec = tabHost.newTabSpec("mainTab");
        tabSpec.setContent(R.id.mainTab);
        tabSpec.setIndicator(getString(R.string.mainTab));
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("exchangeTab");
        tabSpec.setContent(R.id.exchangeTab);
        tabSpec.setIndicator(getString(R.string.exchangeTab));
        tabHost.addTab(tabSpec);

        tabHost.setCurrentTab(0);

        readSettings();

        changeExchangeMethod();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        toPreviousActivity();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backBtn:
                toPreviousActivity();
                break;

            case R.id.btnSaveSettings:
                Log.d(TAG, "onClick: btnSaveSettings");

                String title = getString(R.string.save);
                String message = getString(R.string.save_settings);
                DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.SAVE_SETTINGS, title, message);

                break;

            case R.id.btnAbout:
                Log.d(TAG, "onClick: btnAbout");

                String title1 = getString(R.string.about);
                String message1 = String.format("Version: %s\n%s\nAuthor: %s",BuildConfig.VERSION_NAME, "2022", "androskin@ukr.net");
                DialogMessage.showMessage(getSupportFragmentManager(), title1, message1);

                break;

            case R.id.useCamera:
                changeUseCamera();
                break;

            case R.id.btnChooseLanguage:
                showChangeLanguageDialog();
                break;

            case R.id.radioButtonLan:
                exchangeMethod = ExchangeMethods.LAN;
                changeExchangeMethod();
                break;

            case R.id.radioButtonFtp:
                exchangeMethod = ExchangeMethods.FTP;
                changeExchangeMethod();
                break;
        }
    }

    private void toPreviousActivity() {
        finish();
    }

    private void changeExchangeMethod() {
        if (exchangeMethod == ExchangeMethods.LAN) {
            lanLayout.setVisibility(View.VISIBLE);
            ftpLayout.setVisibility(View.GONE);
        } else {
            lanLayout.setVisibility(View.GONE);
            ftpLayout.setVisibility(View.VISIBLE);
        }
    }

    public void readSettings() {
        deviceId.setText(settings.getDeviceId());
        useCamera.setChecked(settings.getUseCamera());
        weightPrefix.setText(settings.getWeightBarcodePrefix());
        codeLength.setText(settings.getCodeLength());

        exchangeMethod = settings.getExchangeMethod();
        if (exchangeMethod == ExchangeMethods.FTP) {
            radioButtonFtp.toggle();
        } else {
            radioButtonLan.toggle();
        }

        lanDirectory.setText(settings.getLanDirectory());
        lanUser.setText(settings.getLanUser());
        lanPassword.setText(settings.getLanPassword());

        ftpAddress.setText(settings.getFtpAddress());
        ftpUser.setText(settings.getFtpUser());
        ftpPassword.setText(settings.getFtpPassword());
        ftpDirectory.setText(settings.getFtpDirectory());
        ftpPassiveMode.setChecked(settings.getFtpPassiveMode());
    }

    public void saveSettings() {
        if (deviceId.getText().toString().trim().equals("")) {
            String title = getString(R.string.error);
            String message = String.format("%s\n%s", getString(R.string.settings_not_saved), getString(R.string.device_id_empty));
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            return;
        }

        if (!weightPrefix.getText().toString().trim().equals("")) {
            int locWeightPrefix = Integer.parseInt(weightPrefix.getText().toString());
            if (locWeightPrefix < 20 || locWeightPrefix > 29) {
                String title = getString(R.string.error);
                String message = String.format("%s\n%s", getString(R.string.settings_not_saved), getString(R.string.error_weight_prefix));
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
                return;
            }
        }

        if (!codeLength.getText().toString().trim().equals("") ) {
            int locCodeLength = Integer.parseInt(codeLength.getText().toString());
            if (locCodeLength < 4 || locCodeLength > 6) {
                String title = getString(R.string.error);
                String message = String.format("%s\n%s", getString(R.string.settings_not_saved), getString(R.string.error_code_length));
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
                return;
            }
        }

        try {
            settings.setDeviceId(String.valueOf(deviceId.getText()));
            settings.setUseCamera(useCamera.isChecked());
            settings.setWeightBarcodePrefix(String.valueOf(weightPrefix.getText()));
            settings.setCodeLength(String.valueOf(codeLength.getText()));

            settings.setExchangeMethod(exchangeMethod);

            settings.setLanDirectory(String.valueOf(lanDirectory.getText()));
            settings.setLanUser(String.valueOf(lanUser.getText()));
            settings.setLanPassword(String.valueOf(lanPassword.getText()));

            settings.setFtpAddress(String.valueOf(ftpAddress.getText()));
            settings.setFtpUser(String.valueOf(ftpUser.getText()));
            settings.setFtpPassword(String.valueOf(ftpPassword.getText()));
            settings.setFtpDirectory(String.valueOf(ftpDirectory.getText()));
            settings.setFtpPassiveMode(ftpPassiveMode.isChecked());

            String message = getString(R.string.settings_saved);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            readSettings();
        } catch (Exception ex) {
            String title = getString(R.string.error);
            String message = String.format("%s\n%s", getString(R.string.settings_not_saved), ex.getMessage());
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void showChangeLanguageDialog() {
        String[] languages = settings.getLanguagesList();
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle(R.string.choose_language);
        builder.setSingleChoiceItems(languages, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    settings.setLanguage("en");
                    settings.setLocale(getBaseContext(), settings.getLanguage());
                    saveLanguage(settings.getLanguage());
                    recreate();
                }
                else if (i == 1) {
                    settings.setLanguage("uk");
                    settings.setLocale(getBaseContext(), settings.getLanguage());
                    saveLanguage(settings.getLanguage());
                    recreate();
                }

                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveLanguage(String lang) {
        settings.setLanguage(lang);
    }

    private void changeUseCamera() {
        if (useCamera.isChecked()) {
            if (!CameraUtil.isCameraPermission(this)) {
                CameraUtil.askCameraPermission(this);
            }
        }
    }

}