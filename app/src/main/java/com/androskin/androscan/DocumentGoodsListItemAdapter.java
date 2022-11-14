package com.androskin.androscan;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.androskin.androscan.dialogs.DialogYesNo;
import com.androskin.androscan.enums.EnumDialogValues;

import java.util.ArrayList;

public class DocumentGoodsListItemAdapter extends BaseAdapter implements ListAdapter {
    private final static String TAG = "GA_DocGoodsLstItemAdapt";

    private final ArrayList<String[]> list;
    private final Context context;
    private final FragmentManager fragmentManager;
    private int currentPosition;

    public DocumentGoodsListItemAdapter(Context context, FragmentManager fragmentManager, ArrayList<String[]> list) {
        Log.d(TAG, "new DocumentsGoodsListItemAdapter");
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.list = list;
        this.currentPosition = -1;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(list.get(position)[0]);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.document_goods_list_item, null);
        }

        RelativeLayout itemLayout = view.findViewById(R.id.itemLayout);
        itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onItemClick");
                currentPosition = position;
                fragmentManager.setFragmentResult(EnumDialogValues.DOCUMENT_ITEM_DETAILS.toString(), new Bundle());
            }
        });

        //Handle TextView and display string from your list
        TextView name = view.findViewById(R.id.name);
        name.setText(getItemName(position));

        TextView article = view.findViewById(R.id.article);
        article.setText(getItemArticle(position));

        TextView additional = view.findViewById(R.id.additional);
        additional.setText(getItemAdditional(position));

        TextView code = view.findViewById(R.id.barcode);
        code.setText(getItemBarcode(position));

        TextView barcode = view.findViewById(R.id.amount);
        barcode.setText(getItemAmount(position));

        //Handle buttons and add onClickListeners
        Button deleteItem = view.findViewById(R.id.deleteItem);
        deleteItem.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked button deleteItem id'"+list.get(position)[0]+"'");

                currentPosition = position;

                String title = "Delete current line";
                String message = String.format("Delete item '%s'\nbarcode = %s\namount = %s\n?", getItemName(position), getItemBarcode(position), getItemAmount(position));
                DialogYesNo.showDialog(fragmentManager, EnumDialogValues.DELETE_DOCUMENT_ITEM, title, message);
            }
        });

        return view;
    }

    public ArrayList<String[]> getAllItems() {
        return list;
    }

    public String getItemName(int position) {
        int i = 1;

        if (list.get(position)[i] == null)
            return "";
        else
            return list.get(position)[i];
    }

    public String getItemArticle(int position) {
        int i = 2;

        if (list.get(position)[i] == null)
            return "";
        else
            return list.get(position)[i];
    }

    public String getItemAdditional(int position) {
        int i = 3;

        if (list.get(position)[i] == null)
            return "";
        else
            return list.get(position)[i];
    }

    public String getItemBarcode(int position) {
        int i = 4;

        if (list.get(position)[i] == null)
            return "";
        else
            return list.get(position)[i];
    }

    public String getItemAmount(int position) {
        int i = 5;

        if (list.get(position)[i] == null)
            return "";
        else
            return list.get(position)[i];
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void removePosition(int position) {
        list.remove(position);
    }
}
