package com.androskin.androscan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentResultListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.androskin.androscan.dialogs.DialogYesNo;
import com.androskin.androscan.enums.EnumDialogValues;
import com.androskin.androscan.utils.ActivityUtil;
import com.androskin.androscan.utils.FileUtil;
import com.androskin.androscan.utils.SQLite;
import com.androskin.androscan.utils.SettingsUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = "GA_MainActivity";

    SettingsUtil settings;

    public static SharedPreferences sharedPreferences;

    private Button btnGoods, btnDocuments, btnSettings, btnExit;

    public SQLite sqLite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        sharedPreferences = getSharedPreferences(SettingsUtil.SETTINGS_FILE, Context.MODE_PRIVATE);
        settings = SettingsUtil.getInstance(sharedPreferences);
        settings.setLocale(getBaseContext(), settings.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.EXIT_APP.toString(), this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String key, @NonNull Bundle bundle) {
                finish();
            }
        });

        sqLite = new SQLite(getApplicationContext());

        ActivityUtil.setPortraitOrientation(this);
        FileUtil.verifyStoragePermissions(this);

        TextView title = findViewById(R.id.title);
        String textTitle = getString(R.string.app_title)+" "+BuildConfig.VERSION_NAME;
        title.setText(textTitle);

        btnGoods = findViewById(R.id.btnGoods);
        btnGoods.setOnClickListener(this);

        btnDocuments = findViewById(R.id.btnDocuments);
        btnDocuments.setOnClickListener(this);

        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(this);

        btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(this);

        getIntent().setAction("MainActivity - Already created");
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        exitApp();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //restart to updating Local
        String action = getIntent().getAction();
        if(action == null || !action.equals("MainActivity - Already created")) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        else {
            getIntent().setAction(null);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.btnGoods:
                Log.d(TAG, "onClick: btnGoods");

                Intent goodsIntent = new Intent(this, GoodsActivity.class);
                startActivity(goodsIntent);

                break;
            case R.id.btnDocuments:
                Log.d(TAG, "onClick: btnDocuments");

                Intent documentsIntent = new Intent(this, DocumentsActivity.class);
                startActivity(documentsIntent);

                break;
            case R.id.btnSettings:
                Log.d(TAG, "onClick: btnSettings");

                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);

                break;

            case R.id.btnExit:
                Log.d(TAG, "onClick: btnExit");

                exitApp();

                break;
        }

        sqLite.close();
    }

    private void exitApp() {
        String title = getString(R.string.exit);
        String message = getString(R.string.exit_question);
        DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.EXIT_APP, title, message);
    }
}