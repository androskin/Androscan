package com.androskin.androscan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentResultListener;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.androskin.androscan.dialogs.DialogMessage;
import com.androskin.androscan.dialogs.DialogYesNo;
import com.androskin.androscan.enums.EnumDialogValues;
import com.androskin.androscan.utils.ActivityUtil;
import com.androskin.androscan.utils.SQLite;
import com.androskin.androscan.utils.SettingsUtil;

public class DocumentsActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = "GA_DocumentsActivity";

    SettingsUtil settings;

    private Button backBtn;
    private Button newDocument;

    private EditText filter;
    private Button filterClear;

    private ListView documentsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        settings = SettingsUtil.getInstance(MainActivity.sharedPreferences);
        settings.setLocale(getBaseContext(), settings.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        ActivityUtil.setPortraitOrientation(this);

        getSupportFragmentManager().setFragmentResultListener(EnumDialogValues.CREATE_NEW_DOCUMENT.toString(), this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String key, @NonNull Bundle bundle) {
                createNewDocument();
            }
        });

        //hidden keyboard on start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(this);

        newDocument = findViewById(R.id.newDocument);
        newDocument.setOnClickListener(this);

        filter = findViewById(R.id.filter);
        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateDocumentsList();
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

        documentsList = findViewById(R.id.documentsList);
        documentsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                openDocument(id);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        updateDocumentsList();
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

            case R.id.newDocument:
                String title = getString(R.string.documents);
                String message = getString(R.string.documents_new_document);
                DialogYesNo.showDialog(getSupportFragmentManager(), EnumDialogValues.CREATE_NEW_DOCUMENT, title, message);
                break;

            case R.id.filterClear:
                filter.setText("");
                updateDocumentsList();
                break;
        }
    }

    private void toPreviousActivity() {
        finish();
    }

    private void createNewDocument() {
        try (SQLite sqLite = new SQLite(getApplicationContext())) {
            long newDocID = sqLite.newDocument();
            if (newDocID != -1) {
                Intent documentIntent = new Intent(this, DocumentActivity.class);
                documentIntent.putExtra("doc_id", newDocID);
                startActivity(documentIntent);
            } else {
                DialogMessage.showMessage(getSupportFragmentManager(), getString(R.string.error), getString(R.string.error_create_new_document));
            }
        } catch (Exception e) {
            DialogMessage.showMessage(getSupportFragmentManager(), getString(R.string.error), getString(R.string.error_create_new_document));
        }
    }

    private void openDocument(long id) {
        if (id > 0) {
            Intent documentIntent = new Intent(this, DocumentActivity.class);
            documentIntent.putExtra("doc_id", id);
            startActivity(documentIntent);
        } else {
            DialogMessage.showMessage(getSupportFragmentManager(), getString(R.string.error), getString(R.string.error_open_document));
        }
    }

    private void updateDocumentsList() {
        try (SQLite sqLite = new SQLite(getApplicationContext())) {

            SimpleCursorAdapter adapter = sqLite.readDocuments(this, filter.getText().toString());
            documentsList.setAdapter(adapter);

        } catch (Exception e) {
            DialogMessage.showMessage(getSupportFragmentManager(), getString(R.string.error), getString(R.string.error_open_document));
            Toast.makeText(this, getString(R.string.error_read_documents_list)+"\n"+e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}