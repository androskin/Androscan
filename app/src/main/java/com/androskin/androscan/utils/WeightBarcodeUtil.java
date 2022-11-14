package com.androskin.androscan.utils;

import android.util.Log;

import com.androskin.androscan.MainActivity;

public class WeightBarcodeUtil {
    private final static String TAG = "WeightBarcodeUtil";

    private String barcode;
    private double weight;

    public WeightBarcodeUtil(String barcode, double weight) {
        this.barcode = barcode;
        this.weight = weight;
    }

    public String getBarcode() {
        return barcode;
    }

    public double getWeight() {
        return weight;
    }

    public void checkWeightBarcode() {
        barcode = barcode.trim();

        if(!(barcode.length() == 13)) return;

        SettingsUtil settings = SettingsUtil.getInstance(MainActivity.sharedPreferences);

        if(!settings.getWeightBarcodePrefix().equals("") && !settings.getCodeLength().equals("")) {
            try {

                //check barcode prefix
                int locWeightPrefix = Integer.parseInt(settings.getWeightBarcodePrefix());
                if (locWeightPrefix < 20 || locWeightPrefix > 29)  return;

                //check code length
                int locCodeLength = Integer.parseInt(settings.getCodeLength());
                if (locCodeLength < 4 || locCodeLength > 6) return;


                String prefix = barcode.substring(0, 2);

                if (prefix.equals(settings.getWeightBarcodePrefix())) {
                    String locBarcode = String.valueOf(Integer.parseInt(barcode.substring(2, 2 + locCodeLength)));
                    double locWeight = Double.parseDouble(barcode.substring(2 + locCodeLength, 12))/1000;

                    barcode = locBarcode;
                    weight = locWeight;
                }

            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }
}
