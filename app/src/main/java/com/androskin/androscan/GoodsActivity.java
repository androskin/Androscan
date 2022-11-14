package com.androskin.androscan;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentResultListener;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.androskin.androscan.dialogs.DialogMessage;
import com.androskin.androscan.dialogs.DialogYesNo;
import com.androskin.androscan.enums.EnumDialogValues;
import com.androskin.androscan.enums.ExchangeMethods;
import com.androskin.androscan.utils.ActivityUtil;
import com.androskin.androscan.utils.CameraUtil;
import com.androskin.androscan.utils.FileUtil;
import com.androskin.androscan.utils.FtpClientUtil;
import com.androskin.androscan.utils.MessageUtil;
import com.androskin.androscan.utils.SQLite;
import com.androskin.androscan.utils.SettingsUtil;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;

public class GoodsActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = "GA_GoodsActivity";

    private ListView goodsList;
    private Button backBtn;
    private Button downloadGoods;

    private TextView totalCount;

    private EditText filter;
    private Button filterClear;
    private ImageButton filterScan;

    private List<String> goodsForDownload;

    SettingsUtil settings;

    private FtpClientUtil ftpClient = null;
    private ProgressDialog pd;
    private final Handler handler = new Handler(Looper.getMainLooper()) {

        public void handleMessage(android.os.Message msg) {

            if (msg.what == 0) {
                //Success
                if (pd != null && pd.isShowing()) {pd.dismiss();}
            } else if (msg.what == 1) {
                //Download file from FTP
                if (pd != null && pd.isShowing()) {pd.dismiss();}
                downloadGoodsFile();
            } else if (msg.what == 2) {
                //From file to list
                if (pd != null && pd.isShowing()) {pd.dismiss();}
                goodsToList();
            } else if (msg.what == 3) {
                //From list to SQL
                if (pd != null && pd.isShowing()) {pd.dismiss();}
                listToSQL();
            } else if (msg.what == 4) {
                //Finish download, update activity
                if (pd != null && pd.isShowing()) {pd.dismiss();}
                onResume();
            } else if (msg.what == 100) {
                //Progressbar
                pd.setIndeterminate(false);
                if (pd.getProgress() < pd.getMax()) {
                    pd.incrementProgressBy(1);
                }
            } else {
                //Something else, show message
                String title = "";
                String message;

                if (pd != null && pd.isShowing()) {pd.dismiss();}

                try{
                    title = msg.getData().get("title").toString();
                    message = msg.getData().get("msg").toString();
                } catch (Exception e) {
                    message = GoodsActivity.this.getString(R.string.unable_to_perform_action);
                }
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            }
        }

    };

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    String barcode = result.getContents();
                    Log.d(TAG, "Scanned: "+barcode);

                    filter.setText(barcode);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        settings = SettingsUtil.getInstance(MainActivity.sharedPreferences);
        settings.setLocale(getBaseContext(), settings.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods);

        ActivityUtil.setPortraitOrientation(this);

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.DOWNLOAD_GOODS.toString(), this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String key, @NonNull Bundle bundle) {
                startGoodsUpdating();
            }
        });

        //hidden keyboard on start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        ftpClient = new FtpClientUtil();

        filter = findViewById(R.id.filter);
        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateGoodsList();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        filter.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if( keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                    return true;
                }
                return false;
            }
        });

        filterClear = findViewById(R.id.filterClear);
        filterClear.setOnClickListener(this);

        filterScan = findViewById(R.id.filterScan);
        filterScan.setOnClickListener(this);
        if (!settings.getUseCamera()) {
            filterScan.setVisibility(View.GONE);
        }

        goodsList = findViewById(R.id.goods_list);
        goodsList.setOnItemClickListener((adapterView, view, position, id) -> showItemDetails(id));

        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(this);

        downloadGoods = findViewById(R.id.downloadGoods);
        downloadGoods.setOnClickListener(this);

        totalCount = findViewById(R.id.totalCount);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateGoodsList();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        toPreviousActivity();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.backBtn:
                toPreviousActivity();
                break;

            case R.id.downloadGoods:
                String title = getString(R.string.download);
                String message = getString(R.string.download_from_ftp);
                DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.DOWNLOAD_GOODS, title, message);
                break;

            case R.id.filterClear:
                filter.setText("");
                updateGoodsList();
                break;

            case R.id.filterScan:
                if (settings.getUseCamera()) {
                    if (!CameraUtil.isCameraPermission(this)) {
                        Toast.makeText(this, R.string.error_camera_permission_denied, Toast.LENGTH_LONG).show();
                    } else {
                        ScanOptions scanOptions = new ScanOptions();
                        scanOptions.setPrompt("Scan barcode");
                        scanOptions.setOrientationLocked(false);
                        scanOptions.setBeepEnabled(true);
                        barcodeLauncher.launch(scanOptions);
                    }
                }
                break;
        }
    }

    private void toPreviousActivity() {
        finish();
    }

    private void startGoodsUpdating() {
        final String lanPath = settings.getLanDirectory().trim();
        final String host = settings.getFtpAddress().trim();
        final String username = settings.getFtpUser().trim();
        final String password = settings.getFtpPassword().trim();
        final String path = settings.getFtpDirectory().trim();

        FileUtil.deleteFile(FileUtil.DESTINATION_GOODS_FILE);

        if(settings.getExchangeMethod() == ExchangeMethods.LAN) {

            if (lanPath.trim().equals("")) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_check_lan_directory);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            } else {
                handler.sendEmptyMessage(1);
            }

        } else if (settings.getExchangeMethod() == ExchangeMethods.FTP) {

            String checkFtpSettings = settings.checkFtpSettings();
            if (checkFtpSettings.equals("")) {

                if (pd != null && pd.isShowing()) {
                    pd.dismiss();
                }
                pd = ProgressDialog.show(GoodsActivity.this, "", getString(R.string.connecting),
                        true, false);

                new Thread(new Runnable() {
                    public void run() {
                        boolean status;
                        try {
                            status = ftpClient.ftpConnect(host, username, password, 21);

                            if (status) {
                                Log.d(TAG, "Connection Success");
                            } else {
                                Log.d(TAG, "Connection failed");
                                String msg = getString(R.string.connection_to_ftp_failed);
                                handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                                return;
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Connection failed");
                            String msg = getString(R.string.connection_to_ftp_failed)+"\n"+e.getMessage();
                            handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                            return;
                        }

                        if (settings.getFtpPassiveMode()) {
                            ftpClient.mFTPClient.enterLocalPassiveMode();
                        }

                        if (!path.equals("")) {
                            status = ftpClient.ftpChangeDirectory(path);
                        }
                        if (!status) {
                            ftpClient.ftpDisconnect();
                            String msg = getString(R.string.error_ftp_change_directory);
                            handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                            return;
                        }

                        handler.sendEmptyMessage(1);
                    }
                }).start();
            } else {
                String title = getString(R.string.error);
                DialogMessage.showMessage(getSupportFragmentManager(), title, checkFtpSettings);
            }

        } else {
            String title = getString(R.string.error);
            String message = getString(R.string.error_check_exchange_method);
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void downloadGoodsFile() {
        final String file = FileUtil.GOODS_FILE;
        final String lanPath = settings.getLanDirectory().trim();
        final String ftpPath = settings.getFtpDirectory().trim();

        if (pd != null && pd.isShowing()) {pd.dismiss();}
        pd = ProgressDialog.show(GoodsActivity.this, "", getString(R.string.downloading),
                true, false);

        if (settings.getExchangeMethod() == ExchangeMethods.LAN) {

            if (lanPath.trim().equals("")) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_check_lan_directory);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);

            } else {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            FileUtil.sambaDownloadFile(GoodsActivity.this, lanPath + file, FileUtil.DESTINATION_GOODS_FILE);
                        } catch (SmbException e) {
                            String msg = getString(R.string.error_download)+"\n"+e.getMessage();
                            handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                            return;
                        }

                        handler.sendEmptyMessage(2);

                    }
                }).start();
            }

        } else if (settings.getExchangeMethod() == ExchangeMethods.FTP) {

            new Thread(new Runnable() {
                public void run() {
                    boolean status;

                    status = ftpClient.ftpDownload(ftpPath + file, FileUtil.DESTINATION_GOODS_FILE);
                    if (!status) {
                        String msg = getString(R.string.error_download) + "\n" + ftpClient.showServerReply();
                        ftpClient.ftpDisconnect();
                        handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                        return;
                    }

                    ftpClient.ftpDisconnect();

                    handler.sendEmptyMessage(2);
                }
            }).start();

        } else {
            String title = getString(R.string.error);
            String message = getString(R.string.error_check_exchange_method);
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void goodsToList() {
        String file = FileUtil.DESTINATION_GOODS_FILE;
        goodsForDownload = new ArrayList<>();

        if (pd != null && pd.isShowing()) {pd.dismiss();}
        pd = ProgressDialog.show(GoodsActivity.this, "", getString(R.string.updating_database),
                true, false);

        new Thread(new Runnable() {
            public void run() {
                int lineNumber = 0;
                Charset charset = Charset.forName("windows-1251");
                try (FileInputStream fileInputStream = new FileInputStream(file);
                     InputStreamReader reader = new InputStreamReader(fileInputStream, charset);
                     BufferedReader bufferedReader = new BufferedReader(reader)) {

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        lineNumber++;
                        goodsForDownload.add(line);
                    }
                } catch (Exception e) {
                    String msg = String.format(
                            GoodsActivity.this.getString(R.string.error_goods_to_list)+"\n"+
                            GoodsActivity.this.getString(R.string.line)+": %s\n"+
                            GoodsActivity.this.getString(R.string.message)+": %s\n",
                            lineNumber,
                            e.getMessage());
                    handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                }

                Log.d(TAG, "File LINES - "+lineNumber);
                handler.sendEmptyMessage(3);
            }
        }).start();
    }

    private void listToSQL() {
        if (goodsForDownload.size() == 0) return;

        if (pd != null && pd.isShowing()) {pd.dismiss();}
        pd = new ProgressDialog(GoodsActivity.this);
        pd.setMessage(getString(R.string.updating_database));
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMax(goodsForDownload.size());
        pd.setIndeterminate(true);
        pd.show();

        new Thread(new Runnable() {
            public void run() {
                int lineNumber = 0;
                try (SQLite sqLite = new SQLite(getApplicationContext())) {
                    String line;
                    String[] splitLine;

                    String code;
                    String name;
                    String article;
                    String additional;
                    String unit;
                    int divisible;
                    String barcode;
                    double price;
                    double amount;

                    for (int i = 0; i < goodsForDownload.size(); i++) {
                        line = goodsForDownload.get(i);

                        lineNumber++;
                        Log.d(TAG, line);

                        //ProgressDialog
                        handler.sendEmptyMessage(100);

                        line = line.replaceAll("'", "`");

                        splitLine = line.split(";");

                        code = splitLine[0].trim();
                        name = splitLine[1].trim();
                        article = splitLine[2].trim();
                        additional = splitLine[3].trim();
                        unit = splitLine[4].trim();
                        divisible = Integer.parseInt(splitLine[5]);
                        barcode = splitLine[6].trim();
                        price = Double.parseDouble(splitLine[7]);
                        amount = Double.parseDouble(splitLine[8]);

                        sqLite.updateGood(code, name, article, additional, unit, divisible, barcode, price, amount);
                    }
                } catch (Exception e) {
                    String msg = String.format(
                            GoodsActivity.this.getString(R.string.error_list_to_sql)+"\n"+
                            GoodsActivity.this.getString(R.string.line)+": %s\n"+
                            GoodsActivity.this.getString(R.string.message)+": %s\n",
                            lineNumber,
                            e.getMessage());
                    handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                }

                Log.d(TAG, "goodsForDownload LINES - "+lineNumber);
                handler.sendEmptyMessage(4);
            }
        }).start();
    }

    private void updateGoodsList() {
        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            SimpleCursorAdapter adapter = sqLite.readGoods(this, filter.getText().toString());
            goodsList.setAdapter(adapter);
        } catch (Exception e) {
            String title = getString(R.string.error);
            String msg = getString(R.string.error_update_goods_list)+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, msg);
        }

        updateTotalCount();
    }

    private void showItemDetails(long id) {
        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            Goods goods = sqLite.getGoodsDetails(id);
            String msg = sqLite.getGoodsDetailsString(this, goods);
            handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.item_details), msg));
        } catch (Exception e) {
            String title = getString(R.string.error);
            String msg = getString(R.string.error_edit_barcode)+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, msg);
        }
    }

    private void updateTotalCount() {
        int total = 0;

        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            total = sqLite.getGoodsRowCount();
        } catch (Exception e) {
            String title = getString(R.string.error);
            String msg = "Error - Update goods row count."+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, msg);
        }

        totalCount.setText(String.valueOf(total));
    }

}