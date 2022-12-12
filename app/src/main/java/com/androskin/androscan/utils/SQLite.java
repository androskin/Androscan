package com.androskin.androscan.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.androskin.androscan.DocumentGoodsListItemAdapter;
import com.androskin.androscan.Goods;
import com.androskin.androscan.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SQLite extends SQLiteOpenHelper {
    private final static String TAG = "GA_SQLite";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "androscan.db";

    public SQLite(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");

        String createTableGoods =
                "CREATE TABLE IF NOT EXISTS goods "+
                    "("+
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                        "code TEXT, "+
                        "name TEXT, "+
                        "article TEXT, "+
                        "additional TEXT, "+
                        "unit TEXT, "+
                        "divisible INTEGER, "+
                        "barcode TEXT, "+
                        "price REAL,"+
                        "amount REAL"+
                    ");";
        db.execSQL(createTableGoods);

        String createTableDocuments =
                "CREATE TABLE IF NOT EXISTS documents "+
                    "("+
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                        "date TEXT, "+
                        "comments TEXT"+
                    ");";
        db.execSQL(createTableDocuments);

        String createTableDocumentGoods =
                "CREATE TABLE IF NOT EXISTS document_goods "+
                    "("+
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                        "document_id INTEGER, "+
                        "goods_id INTEGER, "+
                        "barcode TEXT, "+
                        "amount REAL, "+
                        "FOREIGN KEY (document_id)  REFERENCES documents (_id), "+
                        "FOREIGN KEY (goods_id)  REFERENCES goods (_id)"+
                    ");";
        db.execSQL(createTableDocumentGoods);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS document_goods");
        db.execSQL("DROP TABLE IF EXISTS documents");
        db.execSQL("DROP TABLE IF EXISTS goods");

        onCreate(db);
    }

    public SimpleCursorAdapter readGoods(Context context, String filter) {
        SQLiteDatabase db = this.getReadableDatabase();

        String filterText = filter.trim();
        String filterRequest = "";
        if(!filter.trim().equals("")) {
            filterRequest =
                    "WHERE (\n"+
                            "code LIKE '%"+filterText+"%'\n"+
                            "OR name LIKE '%"+filterText+"%'\n"+
                            "OR article LIKE '%"+filterText+"%'\n"+
                            "OR additional LIKE '%"+filterText+"%'\n"+
                            "OR barcode LIKE '%"+filterText+"%'\n"+
                            ")\n";
        }

        String sqlRequest = "SELECT * FROM goods "+filterRequest+" ORDER BY name";

        Cursor cursor = db.rawQuery(sqlRequest, null);

        String[] header = new String[]{"name", "article", "additional", "code", "barcode"};
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(context, R.layout.goods_list_item,
                cursor, header, new int[]{R.id.name, R.id.article, R.id.additional, R.id.code, R.id.barcode}, 0);

        return adapter;
    }

    public DocumentGoodsListItemAdapter readDocumentGoods(Context context, FragmentManager fragmentManager, long documentId, String filter) {
        SQLiteDatabase db = this.getReadableDatabase();

        String filterText = filter.trim();
        String filterRequest = "";
        if(!filter.trim().equals("")) {
            filterRequest =
                    "AND (\n"+
                            "goods.name LIKE '%"+filterText+"%'\n"+
                            "OR goods.article LIKE '%"+filterText+"%'\n"+
                            "OR goods.additional LIKE '%"+filterText+"%'\n"+
                            "OR document_goods.barcode LIKE '%"+filterText+"%'\n"+
                            "OR goods.code LIKE '%"+filterText+"%'\n"+
                        ")\n";
        }

        String sqlRequest =
                    "SELECT document_goods._id, goods.name, goods.article, goods.additional, document_goods.barcode, document_goods.amount \n"+
                    "FROM document_goods \n"+
                    "LEFT JOIN goods \n"+
                    "ON document_goods.goods_id = goods._id \n"+
                    "WHERE \n"+
                    "document_id='"+documentId+"'\n"+
                    filterRequest+
                    "ORDER BY document_goods._id DESC";

        Cursor cursor = db.rawQuery(sqlRequest, null);

        ArrayList<String[]> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(new String[]{
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5)
            });
        }

        cursor.close();

        DocumentGoodsListItemAdapter adapter = new DocumentGoodsListItemAdapter(context, fragmentManager, list);

        return adapter;
    }

    public void updateGood(String code, String name, String article, String additional, String unit, int divisible, String barcode, double price, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sqlRequest;

        if (code.equals("")) return;

        if (barcode.equals("")) barcode = code;

        sqlRequest =
                "UPDATE  goods \n"+
                "SET \n"+
                    "code = '"+code+"', \n"+
                    "name = '"+name+"', \n"+
                    "article = '"+article+"', \n"+
                    "additional = '"+additional+"', \n"+
                    "unit = '"+unit+"', \n"+
                    "divisible = "+divisible+", \n"+
                    "barcode = '"+barcode+"', \n"+
                    "price = "+price+", \n"+
                    "amount = "+amount+" \n"+
                "WHERE   barcode = '"+barcode+"'; \n";
        db.execSQL(sqlRequest);

        sqlRequest =
                "INSERT INTO goods (code, name, article, additional, unit, divisible, barcode, price, amount) \n"+
                "SELECT \n"+
                    "'"+code+ "', \n"+
                    "'"+name+ "', \n"+
                    "'"+article+ "', \n"+
                    "'"+additional+ "', \n"+
                    "'"+unit+ "', \n"+
                    divisible+ ", \n"+
                    "'"+barcode+ "', \n"+
                    price+ ", \n"+
                    amount+" \n"+
                "WHERE '"+barcode+"' NOT IN \n"+
                    "( \n"+
                        "SELECT  barcode \n"+
                        "FROM    goods \n"+
                        "WHERE   barcode = '"+barcode+"' \n"+
                    ");";
        db.execSQL(sqlRequest);
    }

    public SimpleCursorAdapter readDocuments(Context context, String filter) {
        SQLiteDatabase db = this.getReadableDatabase();

        String filterText = filter.trim();
        String filterRequest = "";
        if(!filter.trim().equals("")) {
            filterRequest =
                    "WHERE (\n"+
                            "_id LIKE '%"+filterText+"%'\n"+
                            "OR date LIKE '%"+filterText+"%'\n"+
                            "OR comments LIKE '%"+filterText+"%'\n"+
                            ")\n";
        }

        String sqlRequest = "SELECT * FROM documents "+filterRequest+" ORDER BY _id DESC";

        Cursor cursor = db.rawQuery(sqlRequest, null);

        String[] header = new String[]{"comments", "_id", "date"};
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(context, R.layout.documents_list_item,
                cursor, header, new int[]{R.id.text1, R.id.text2, R.id.text3}, 0);

        return adapter;
    }

    public long newDocument() {
        long result;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("date", DateUtil.getString(DateUtil.getCurrentDate()));

        result = db.insert("documents", null, cv);

        return result;
    }

    public long updateDocumentDetail(long id, String column, String value) {
        long result = -1;

        if (id > 0) {
            SQLiteDatabase db = this.getReadableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(column, value);

            result = db.update("documents", cv, "_id=?", new String[]{String.valueOf(id)});
        }

        return result;
    }

    public long deleteDocument(long id) {
        long result = -1;

        SQLiteDatabase db = this.getReadableDatabase();

        if (id > 0) {
            result = db.delete("documents", "_id=?", new String[]{String.valueOf(id)});
        }

        return result;
    }

    public Map<String, String> getDocumentDetailsById(long id) {
        Map<String, String> result = new HashMap<>();
        result.put("id", String.valueOf(id));
        result.put("date", "");
        result.put("comments", "");

        if (id > 0) {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.rawQuery("SELECT date, comments FROM documents WHERE documents._id = '"+id+"'", null);

            if(cursor.moveToNext()) {
                result.put("date", cursor.getString(0));
                result.put("comments", cursor.getString(1));
            }

            cursor.close();
        }

        return result;
    }

    public double getCurrentAmount(String docId, String barcode) {
        double result = 0;

        if (!barcode.equals("")) {
            SQLiteDatabase db = this.getReadableDatabase();

            String sqlRequest =
                    "SELECT SUM(document_goods.amount) \n" +
                    "FROM document_goods \n" +
                    "LEFT JOIN goods \n"+
                    "ON document_goods.goods_id = goods._id \n"+
                    "WHERE \n" +
                    "document_goods.document_id = '"+docId+"' \n" +
                    "and ((goods.code = '"+barcode+"') or (goods.barcode = '"+barcode+"') or (document_goods.barcode = '"+barcode+"'))";

            Cursor cursor = db.rawQuery(sqlRequest, null);

            if(cursor.moveToNext()) {
                result = cursor.getDouble(0);
            }

            cursor.close();
        }

        result = BigDecimal.valueOf(result)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();

        return result;
    }

    public long getGoodsId(String barcode) {
        long result = 0;

        barcode = barcode.trim();
        if (!barcode.equals("")) {
            SQLiteDatabase db = this.getReadableDatabase();

            //find by barcode
            try (Cursor cursor = db.rawQuery("SELECT _id FROM goods WHERE goods.barcode = '"+barcode+"'", null)) {
                if (cursor.moveToNext()) {
                    result = cursor.getLong(0);
                    return result;
                }
            }

            //find by code
            try (Cursor cursor = db.rawQuery("SELECT _id FROM goods WHERE goods.code = '"+barcode+"'", null)) {
                if (cursor.getCount() > 0) {
                    if (cursor.moveToNext()) {
                        result = cursor.getLong(0);
                        return result;
                    }
                }
            }
        }

        return result;
    }

    public String getGoodsBarcode(long id) {
        String result = "";

        if (!(id <= 0)) {
            SQLiteDatabase db = this.getReadableDatabase();

            try (Cursor cursor = db.rawQuery("SELECT code, barcode FROM goods WHERE goods._id = '"+id+"'", null)) {
                if (cursor.moveToNext()) {
                    result = cursor.getString(1);

                    if (result.equals("")) {
                        result = cursor.getString(0);
                    }

                    return result;
                }
            }
        }

        return result;
    }

    @SuppressLint("DefaultLocale")
    public String getGoodsDetailsString(Context context, Goods goods) {
        String result = context.getString(R.string.unknown_goods);

        if (goods == null) {
            return result;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(context.getString(R.string.goods_code)+": %s\n", goods.getCode()));
        sb.append(String.format(context.getString(R.string.goods_name)+": %s\n", goods.getName()));
        sb.append(String.format(context.getString(R.string.goods_article)+": %s\n", goods.getArticle()));
        sb.append(String.format(context.getString(R.string.goods_additional)+": %s\n", goods.getAdditional()));
        sb.append(String.format(context.getString(R.string.goods_unit)+": %s\n", goods.getUnit()));
        sb.append(String.format(context.getString(R.string.goods_divisible)+": %s\n", goods.isDivisible()?context.getString(R.string.yes):context.getString(R.string.no)));
        sb.append(String.format(context.getString(R.string.goods_barcode)+": %s\n", goods.getBarcode()));
        sb.append(String.format(context.getString(R.string.goods_price)+": %.2f", goods.getPrice()));
        result = sb.toString();

        return result;
    }

    public Goods getGoodsDetails(long id) {
        Goods result = null;

        if (id != 0) {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor;

            cursor = db.rawQuery("SELECT code, name, article, additional, unit, divisible, barcode, price, amount FROM goods WHERE goods._id = '"+id+"'", null);
            if(cursor.moveToNext()) {
                result = new Goods(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5),
                        cursor.getString(6),
                        cursor.getDouble(7),
                        cursor.getDouble(8)
                );
            }

            cursor.close();
        }

        return result;
    }

    public long addGoodsToDocument(long docId, long goodsId, String barcode, double amount) {
        long result;

        if (docId == 0) return -2;

        barcode = barcode.trim();
        if (barcode.equals("")) return -3;

        if (amount <= 0) return -4;

        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("document_id", docId);
        cv.put("goods_id", goodsId);
        cv.put("barcode", barcode);
        cv.put("amount", amount);

        result = db.insert("document_goods", null, cv);

        return result;
    }

    public long deleteGoodsFromDocument(long id) {
        long result = -1;

        SQLiteDatabase db = this.getReadableDatabase();

        if (id > 0) {
            result = db.delete("document_goods", "_id=?", new String[]{String.valueOf(id)});
        }

        return result;
    }

    public long updateGoodsAmount(long goodsId, double amount) {
        long result = -1;

        if (goodsId > 0) {
            SQLiteDatabase db = this.getReadableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("amount", amount);

            result = db.update("document_goods", cv, "_id=?", new String[]{String.valueOf(goodsId)});
        }

        return result;
    }

    public long deleteItemFromDocumentGoods(long goodsId) {
        long result = -1;

        if (goodsId > 0) {
            SQLiteDatabase db = this.getReadableDatabase();

            result = db.delete("document_goods", "_id=?", new String[]{String.valueOf(goodsId)});
        }

        return result;
    }

    public int getGoodsRowCount() {
        int result = 0;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor;

        cursor = db.rawQuery("SELECT COUNT(*) FROM goods", null);
        if(cursor.moveToNext()) {
            result = cursor.getInt(0);
        }

        cursor.close();

        return result;

    }
}
