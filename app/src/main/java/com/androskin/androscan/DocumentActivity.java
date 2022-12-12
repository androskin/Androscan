package com.androskin.androscan;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.androskin.androscan.dialogs.DialogMessage;
import com.androskin.androscan.dialogs.DialogYesNo;
import com.androskin.androscan.enums.EnumDialogValues;
import com.androskin.androscan.enums.ExchangeMethods;
import com.androskin.androscan.utils.ActivityUtil;
import com.androskin.androscan.utils.CameraUtil;
import com.androskin.androscan.utils.DateUtil;
import com.androskin.androscan.utils.FileUtil;
import com.androskin.androscan.utils.FtpClientUtil;
import com.androskin.androscan.utils.MessageUtil;
import com.androskin.androscan.utils.SQLite;
import com.androskin.androscan.utils.SettingsUtil;
import com.androskin.androscan.utils.WeightBarcodeUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jcifs.smb.SmbException;

public class DocumentActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
    private final static String TAG = "GA_DocumentActivity";

    DocumentGoodsListItemAdapter goodsListAdapter;

    ActivityResultLauncher<Intent> goodsActivityResultLauncher;

    TextView headerTitle;

    String fileName;

    final Calendar myCalendar= Calendar.getInstance();
    DatePickerDialog.OnDateSetListener date;

    private long currentDocumentGoodsId;

    private Button backBtn, uploadDocument, deleteDocument;

    private EditText docId, docDate, docComments;

    private EditText filter;
    private Button filterClear;
    private ImageButton filterScan;

    private ListView goodsList;

    private boolean isCameraOpen;
    private DecoratedBarcodeView barcodeScanner;
    private BeepManager beepManager;
    private boolean switchFlashlight = false;

    private ImageButton addBtn;
    private ImageButton scanBtn;
    private Button setBtn, deleteBtn;
    private EditText barcode, amount, count;
    private TextView currentBarcode, goodsDetails;

    TabHost tabHost;
    TabHost.TabSpec tabSpec;

    SettingsUtil settings;

    private FtpClientUtil ftpClient = null;
    private ProgressDialog pd;
    private final Handler handler = new Handler(Looper.getMainLooper()) {

        public void handleMessage(android.os.Message msg) {

            if (msg.what == 0) {
                //Success
                if (pd != null && pd.isShowing()) {pd.dismiss();}

            } else if (msg.what == 1) {
                //Creating file for uploading
                createFileForUploading();
            } else if (msg.what == 2) {
                //Uploading file on FTP
                uploadFileOnFtp();
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
                    message = getString(R.string.unable_to_perform_action);
                }
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            }
        }

    };

    private BarcodeCallback barcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if(result.getText() == null || result.getText().equals("")) {
                return;
            }

            barcodeScanner.setStatusText(result.getText());

            stopCamera();

            barcode.setText(result.getText());
            editBarcode();
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
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
        setContentView(R.layout.activity_document);

        ActivityUtil.setPortraitOrientation(this);

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.UPLOAD_DOCUMENT.toString(), this, (key, bundle) -> startUploadingFile());

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.DELETE_DOCUMENT_CONFIRM.toString(), this, (key, bundle) -> deleteDocumentConfirm());

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.DELETE_DOCUMENT.toString(), this, (key, bundle) -> deleteDocument());

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.DELETE_DOCUMENT_ITEM.toString(), this, (key, bundle) -> deleteDocumentItem());

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.SET_NEW_ITEM_COUNT.toString(), this, (key, bundle) -> setNewCount());

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.DOCUMENT_ITEM_DETAILS.toString(), this, (key, bundle) -> showDocumentItemDetails());

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.DELETE_ITEM_FROM_DOCUMENT_GOODS.toString(), this, (key, bundle) -> deleteItemFromDocumentGoods());

        //hidden keyboard on start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        goodsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                long id = data.getLongExtra("item_id", 0);
                                addItemToBase(id, "");
                            }
                        }
                    }
                });

        ftpClient = new FtpClientUtil();

        date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH,month);
                myCalendar.set(Calendar.DAY_OF_MONTH,day);
                afterDocDateChanged();
            }
        };

        headerTitle = findViewById(R.id.header_title);

        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(this);

        uploadDocument = findViewById(R.id.uploadDocument);
        uploadDocument.setOnClickListener(this);

        deleteDocument = findViewById(R.id.deleteDocument);
        deleteDocument.setOnClickListener(this);

        docId = findViewById(R.id.docId);

        docDate = findViewById(R.id.docDate);
        docDate.setOnClickListener(this);

        docComments = findViewById(R.id.docComments);
        docComments.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                afterDocCommentsChanged();
            }
        });

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


        barcodeScanner = findViewById(R.id.barcodeScanner);

        if (settings.getUseCamera()) {
            Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.EAN_8, BarcodeFormat.EAN_13, BarcodeFormat.CODE_39, BarcodeFormat.CODE_128);
            barcodeScanner.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
            barcodeScanner.initializeFromIntent(getIntent());
            barcodeScanner.decodeContinuous(barcodeCallback);
        } else {
            barcodeScanner.setVisibility(View.GONE);
        }
        stopCamera();

        beepManager = new BeepManager(this);

        addBtn = findViewById(R.id.addBtn);
        addBtn.setOnClickListener(this);

        scanBtn = findViewById(R.id.scanBtn);
        scanBtn.setOnClickListener(this);
        scanBtn.setOnLongClickListener(this);

        barcode = findViewById(R.id.barcode);
        barcode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if( keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                    editBarcode();
                    return true;
                }

                return false;
            }
        });

        setBtn = findViewById(R.id.setBtn);
        setBtn.setOnClickListener(this);

        deleteBtn = findViewById(R.id.deleteBtn);
        deleteBtn.setOnClickListener(this);

        amount = findViewById(R.id.amount);

        count = findViewById(R.id.count);
        count.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if( keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                    return true;
                }
                return false;
            }
        });

        currentBarcode = findViewById(R.id.currentBarcode);
        goodsDetails = findViewById(R.id.goodsDetails);

        tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        tabSpec = tabHost.newTabSpec("mainTab");
        tabSpec.setContent(R.id.mainTab);
        tabSpec.setIndicator(getString(R.string.main_doc_tab));
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("goodsTab");
        tabSpec.setContent(R.id.goodsTab);
        tabSpec.setIndicator(getString(R.string.goods_doc_tab));
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("scanTab");
        tabSpec.setContent(R.id.scanTab);
        tabSpec.setIndicator(getString(R.string.scan_doc_tab));
        tabHost.addTab(tabSpec);

        tabHost.setCurrentTab(0);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                clearScanTab();
                stopCamera();

                if(s.equals("goodsTab")) {
                    updateGoodsList();
                }
            }
        });


        Bundle arguments = getIntent().getExtras();
        long docID = arguments.getLong("doc_id");
        setDocumentDetails(docID);

        updateFileName();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopCamera();
        barcodeScanner = null;
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        toPreviousActivity();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        String title, message;

        String curBarcode, curCount;

        switch (v.getId()) {
            case R.id.backBtn:
                toPreviousActivity();
                break;

            case R.id.uploadDocument:
                title = getString(R.string.upload);
                message = getString(R.string.upload_document);
                DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.UPLOAD_DOCUMENT, title, message);
                break;

            case R.id.deleteDocument:
                title = getString(R.string.delete);
                message = getString(R.string.delete_document);
                DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.DELETE_DOCUMENT_CONFIRM, title, message);
                break;

            case R.id.docDate:
                new DatePickerDialog(DocumentActivity.this,date,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                break;

            case R.id.addBtn:
                addGoodsFromList();

                break;

            case R.id.scanBtn:
                if (barcode.getText().toString().trim().equals("") && settings.getUseCamera()) {
                    if(isCameraOpen) stopCamera();
                    else startCamera();
                } else {
                    editBarcode();
                }
                break;

            case R.id.setBtn:
                curBarcode = currentBarcode.getText().toString();
                curCount = count.getText().toString();
                if (curBarcode.trim().equals("")) break;

                title = curBarcode;
                message = getString(R.string.set_count)+" = "+curCount+"?";
                DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.SET_NEW_ITEM_COUNT, title, message);
                break;

            case R.id.deleteBtn:
                curBarcode = currentBarcode.getText().toString();

                if (curBarcode.trim().equals("")) break;

                title = getString(R.string.delete);
                message = getString(R.string.delete_current_barcode)+"\n"+curBarcode+"\n?";
                DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.DELETE_ITEM_FROM_DOCUMENT_GOODS, title, message);
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

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.scanBtn:
                if (CameraUtil.hasFlash(this)) {
                    if (switchFlashlight) {
                        barcodeScanner.setTorchOff();
                        switchFlashlight = false;
                    } else {
                        barcodeScanner.setTorchOn();
                        switchFlashlight = true;
                    }
                }
                break;
        }

        return true;
    }

    private void toPreviousActivity() {
        finish();
    }

    private void setDocumentDetails(long id) {
        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            Map<String, String> documentDetails = sqLite.getDocumentDetailsById(id);
            docId.setText(documentDetails.get("id"));
            docDate.setText(documentDetails.get("date"));
            docComments.setText(documentDetails.get("comments"));
        } catch (Exception e) {
            String title = getString(R.string.error);
            String message = getString(R.string.error_set_doc_details)+" "+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void startUploadingFile() {
        final String lanPath = settings.getLanDirectory();
        final String host = settings.getFtpAddress().trim();
        final String username = settings.getFtpUser().trim();
        final String password = settings.getFtpPassword().trim();
        final String path = settings.getFtpDirectory().trim();

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
                pd = ProgressDialog.show(DocumentActivity.this, "", getString(R.string.connecting),
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
                String message = checkFtpSettings.trim();
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            }

        } else {
            String title = getString(R.string.error);
            String message = getString(R.string.error_check_exchange_method);
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void createFileForUploading() {
        if (pd != null && pd.isShowing()) {pd.dismiss();}
        pd = ProgressDialog.show(DocumentActivity.this, "", getString(R.string.creating_file),
                true, false);
        new Thread(new Runnable() {
            public void run() {
                long documentId = Long.parseLong(docId.getText().toString());
                String filterText = "";

                try (SQLite sqLite = new SQLite(getApplicationContext())) {
                    goodsListAdapter = sqLite.readDocumentGoods(DocumentActivity.this , getSupportFragmentManager(), documentId, filterText);
                    ArrayList<String[]> goodsList =  goodsListAdapter.getAllItems();

                    if(goodsList.size() == 0) {
                        String msg = getString(R.string.document_empty);
                        handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.upload_document), msg));
                        return;
                    }

                    StringBuilder sb = new StringBuilder();

                    String comments = docComments.getText().toString();

                    comments = comments.replace(';', ',');
                    comments = comments.replace('\n', '|');

                    sb.append(String.format("%s%s;%s;%s\n",
                            settings.getDeviceId().trim(),
                            documentId,
                            docDate.getText().toString(),
                            comments));

                    for (int i = 0; i < goodsList.size(); i++) {
                        sb.append(String.format("%s;%s\n", goodsListAdapter.getItemBarcode(i), goodsListAdapter.getItemAmount(i)));
                    }

                    if(!FileUtil.createFile(FileUtil.TEMP_DOCUMENT_FILE, sb.toString())) {
                        String msg = getString(R.string.error_creating_file);
                        handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                        return;
                    }

                    handler.sendEmptyMessage(2);

                } catch (Exception e) {
                    String msg = getString(R.string.error_upload_document)+"\n"+e.getMessage();
                    handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                }
            }
        }).start();

    }

    private void uploadFileOnFtp() {
        final String lanPath = settings.getLanDirectory().trim();
        final String ftpPath = settings.getFtpDirectory().trim();

        String srcFile = FileUtil.TEMP_DOCUMENT_FILE;
        String desFile;
        if (settings.getExchangeMethod() == ExchangeMethods.LAN) {
            desFile = lanPath + fileName;
        } else {
            if (ftpPath.equals("")) {
                desFile = "/" + fileName;
            } else {
                desFile = ftpPath + fileName;
            }
        }

        if (pd != null && pd.isShowing()) {pd.dismiss();}
        pd = ProgressDialog.show(DocumentActivity.this, "", getString(R.string.uploading),
                true, false);

        if (settings.getExchangeMethod() == ExchangeMethods.LAN) {

            if (lanPath.trim().equals("")) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_check_lan_directory);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);

            } else {
                new Thread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    public void run() {
                        try {
                            FileUtil.sambaUploadFile(DocumentActivity.this, srcFile, desFile);
                        } catch (SmbException e) {
                            String msg = getString(R.string.error_upload)+"\n"+e.getMessage();
                            handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                            return;
                        }

                        String msg = getString(R.string.file_uploaded);
                        handler.sendMessage(MessageUtil.createMessage(-1, fileName, msg));

                    }
                }).start();
            }

        } else if (settings.getExchangeMethod() == ExchangeMethods.FTP) {

            new Thread(new Runnable() {
                public void run() {
                    boolean status;
                    status = ftpClient.ftpUpload(srcFile, desFile);

                    if (!status) {
                        ftpClient.ftpDisconnect();
                        String msg = getString(R.string.error_upload);
                        handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.error), msg));
                        return;
                    }

                    ftpClient.ftpDisconnect();

                    String msg = getString(R.string.file_uploaded);
                    handler.sendMessage(MessageUtil.createMessage(-1, fileName, msg));
                }
            }).start();

        } else {
            String title = getString(R.string.error);
            String message = getString(R.string.error_check_exchange_method);
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }

    }

    private void deleteDocumentConfirm() {
        String title = getString(R.string.delete);
        String message = getString(R.string.are_you_sure);
        DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.DELETE_DOCUMENT, title, message);
    }

    private void deleteDocument() {
        long result = -1;
        long id = Long.parseLong(docId.getText().toString());

        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            if (id > 0) {
                result = sqLite.deleteDocument(id);
            }
        } catch (Exception e) {
            String title = getString(R.string.error);
            String message = getString(R.string.error_delete_document)+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }

        if (result != -1) {
            Toast.makeText(this, getString(R.string.document_deleted),
                    Toast.LENGTH_LONG).show();

            finish();
        } else {
            String title = getString(R.string.error);
            String message = getString(R.string.error_delete_document);
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void afterDocDateChanged() {
        long result;

        String newDate = DateUtil.getString(myCalendar.getTime());
        docDate.setText(newDate);

        updateFileName();

        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            result = sqLite.updateDocumentDetail(Long.parseLong(docId.getText().toString()), "date", newDate);
            if (result == -1) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_change_date);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            }
        } catch (Exception e) {
            String title = getString(R.string.error);
            String message = getString(R.string.error_change_date)+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void updateGoodsList() {
        long documentId = Long.parseLong(docId.getText().toString());
        String filterText = filter.getText().toString();

        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            goodsListAdapter = sqLite.readDocumentGoods(this , getSupportFragmentManager(), documentId, filterText);
            goodsList.setAdapter(goodsListAdapter);
        } catch (Exception e) {
            String title = getString(R.string.error);
            String message = getString(R.string.error_update_goods_list)+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void showDocumentItemDetails() {
        int position = goodsListAdapter.getCurrentPosition();
        String curDocId = docId.getText().toString();

        if(position != -1) {
            String locBarcode = goodsListAdapter.getItemBarcode(position);

            Log.d(TAG, "Show details barcode = " + locBarcode);

            String msg = "";

            try (SQLite sqLite = new SQLite(getApplicationContext())) {
                long goodsId = sqLite.getGoodsId(locBarcode);
                Goods goods = sqLite.getGoodsDetails(goodsId);
                msg = sqLite.getGoodsDetailsString(this, goods);
                msg += "\n\n";
                msg += getString(R.string.amount)+": "+sqLite.getCurrentAmount(curDocId, locBarcode);
            } catch (Exception e) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_show_items_details)+"\n" + e.getMessage();
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            }

            handler.sendMessage(MessageUtil.createMessage(-1, getString(R.string.scancode)+": "+locBarcode, msg));
        }
    }

    private void deleteDocumentItem() {
        int position = goodsListAdapter.getCurrentPosition();

        if(position != -1) {
            Log.d(TAG, "Deleting item id = " + goodsListAdapter.getItemId(position));

            //remove from base
            long result = -1;
            long id = goodsListAdapter.getItemId(position);

            try (SQLite sqLite = new SQLite(getApplicationContext())) {
                if (id > 0) {
                    result = sqLite.deleteGoodsFromDocument(id);
                }
            } catch (Exception e) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_delete_document_item)+" "+e.getMessage();
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            }

            if (result != -1) {
                Toast.makeText(this, getString(R.string.item_deleted),
                        Toast.LENGTH_LONG).show();

                //remove from list
                goodsListAdapter.removePosition(position);
                goodsList.setAdapter(goodsListAdapter);
            } else {
                String title = getString(R.string.error);
                String message = getString(R.string.error_delete_document_item);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            }
        }
    }

    private void afterDocCommentsChanged() {
        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            long result = sqLite.updateDocumentDetail(Long.parseLong(docId.getText().toString()), "comments", docComments.getText().toString());
            if (result == -1) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_change_comments);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            }
        } catch (Exception e) {
            String title = getString(R.string.error);
            String message = getString(R.string.error_change_comments)+" "+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void addGoodsFromList() {
        Intent goodsIntent = new Intent(this, GoodsActivity.class);
        goodsIntent.putExtra("select", true);

        goodsActivityResultLauncher.launch(goodsIntent);
    }

    private void editBarcode() {
        String locBarcode = barcode.getText().toString();
        locBarcode = locBarcode.replace("\n", "");
        barcode.setText("");

        if (locBarcode.trim().equals("")) return;

        beepManager.playBeepSound();

        count.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        Log.d(TAG, "Reading barcode = "+locBarcode);

        addItemToBase(0, locBarcode);
    }

    private void addItemToBase(long id, String locBarcode) {

        Log.d(TAG, "addIdToBase(): Goods id = " + id);

        String curDocId = docId.getText().toString();

        currentDocumentGoodsId = 0;

        double curAmount;
        double locAmount = 1;

        currentBarcode.setText("");

        try (SQLite sqLite = new SQLite(getApplicationContext())) {

            if (id > 0) {
                locBarcode = sqLite.getGoodsBarcode(id);
            } else if (!locBarcode.equals("")) {
                id = sqLite.getGoodsId(locBarcode);
            } else {
                String title = getString(R.string.error);
                String message = String.format(getString(R.string.error_add_goods_to_document) + "\nGoods id = %s\nGoods barcode = %s", id, locBarcode);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
                return;
            }
            Log.d(TAG, "Goods id = " + id);
            Log.d(TAG, "Goods barcode = " + locBarcode);

            currentBarcode.setText(locBarcode);

            WeightBarcodeUtil weightBarcodeUtil = new WeightBarcodeUtil(locBarcode, locAmount);
            weightBarcodeUtil.checkWeightBarcode();
            locBarcode = weightBarcodeUtil.getBarcode();
            locAmount = weightBarcodeUtil.getWeight();

            Goods goods = sqLite.getGoodsDetails(id);

            goodsDetails.setText(sqLite.getGoodsDetailsString(this, goods));

            count.setText(String.valueOf(locAmount));

            //add to base
            long result = sqLite.addGoodsToDocument(Long.parseLong(docId.getText().toString()), id, locBarcode, locAmount);
            if (result == -1) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_add_goods_to_document);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            } else if (result == -2) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_add_goods_to_document)+"\n"+getString(R.string.document)+": " + curDocId;
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            } else if (result == -3) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_add_goods_to_document)+"\n"+getString(R.string.barcode)+" " + locBarcode;
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            } else if (result == -4) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_add_goods_to_document)+"\n"+getString(R.string.amount)+" " + locAmount;
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            } else {
                curAmount = sqLite.getCurrentAmount(curDocId, locBarcode) - locAmount;
                //amount.setText(String.valueOf(curAmount));
                amount.setText(String.format(Locale.ENGLISH, "%.3f", curAmount));

                if (goods != null) {
                    if (goods.isDivisible() || locAmount == 1) {
                        count.setInputType(InputType.TYPE_CLASS_NUMBER);
                        count.setText(String.format(Locale.ENGLISH, "%d", (int) locAmount));

                        amount.setText(String.format(Locale.ENGLISH, "%d", (int) curAmount));
                    }
                }

                currentDocumentGoodsId = result;
            }

        } catch (Exception e) {
            String title = getString(R.string.error);
            String message = getString(R.string.error_edit_barcode)+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void setNewCount() {
        String locBarcode = currentBarcode.getText().toString();
        if (locBarcode.trim().equals("")) return;

        double locAmount = Double.parseDouble(count.getText().toString());

        Log.d(TAG, "setNewCount barcode = "+locBarcode+", amount = "+locAmount);

        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            //update amount in base
            long result = sqLite.updateGoodsAmount(currentDocumentGoodsId, locAmount);
            if (result == -1) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_set_new_count);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            } else {
                Toast.makeText(this, getString(R.string.amount_updated), Toast.LENGTH_LONG).show();

                barcode.requestFocus();
            }
        } catch (Exception e) {
            String title = getString(R.string.error);
            String message = getString(R.string.error_set_new_count)+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void deleteItemFromDocumentGoods() {
        String locBarcode = currentBarcode.getText().toString();
        if (locBarcode.trim().equals("")) return;

        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            //update amount in base
            long result = sqLite.deleteItemFromDocumentGoods(currentDocumentGoodsId);
            if (result == -1) {
                String title = getString(R.string.error);
                String message = getString(R.string.error_delete_item_from_document);
                DialogMessage.showMessage(getSupportFragmentManager(), title, message);
            } else {
                Toast.makeText(this, getString(R.string.item_deleted), Toast.LENGTH_LONG).show();

                clearScanTab();
            }
        } catch (Exception e) {
            String title = getString(R.string.error);
            String message = getString(R.string.error_delete_item_from_document)+"\n"+e.getMessage();
            DialogMessage.showMessage(getSupportFragmentManager(), title, message);
        }
    }

    private void clearScanTab() {
        barcode.setText("");
        currentBarcode.setText("");
        amount.setText("0");
        count.setText("0");
        goodsDetails.setText("");

        barcode.requestFocus();
    }

    private void startCamera() {
        if (settings.getUseCamera()) {
            if (!CameraUtil.isCameraPermission(this)) {
                Toast.makeText(this, R.string.error_camera_permission_denied, Toast.LENGTH_LONG).show();
            } else {
                barcodeScanner.resume();
                isCameraOpen = true;
                Log.d(TAG, "Camera - resume()");
            }
        }
    }

    private void stopCamera() {
        if (settings.getUseCamera()) {
            barcodeScanner.pause();
            isCameraOpen = false;
            Log.d(TAG, "Camera - pause()");
        }
    }

    private void updateFileName() {
        fileName = String.format("%s%s-%s.csv",
                settings.getDeviceId().trim(),
                docId.getText().toString(),
                docDate.getText().toString());

        String header = getString(R.string.document)+" "+fileName;
        headerTitle.setText(header);
    }
}