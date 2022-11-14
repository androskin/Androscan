package com.androskin.androscan;

public class Goods {
    private final String code;
    private final String name;
    private final String article;
    private final String additional;
    private final String unit;
    private final int divisible;
    private final String barcode;
    private final double price;
    private final double amount;

    public Goods(String code, String name, String article, String additional, String unit, int divisible, String barcode, double price, double amount) {
        this.code = code;
        this.name = name;
        this.article = article;
        this.additional = additional;
        this.unit = unit;
        this.divisible = divisible;
        this.barcode = barcode;
        this.price = price;
        this.amount = amount;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getArticle() {
        return article;
    }

    public String getAdditional() {
        return additional;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isDivisible() {
        return divisible == 1;
    }

    public String getBarcode() {
        return barcode;
    }

    public double getPrice() {
        return price;
    }

    public double getAmount() {
        return amount;
    }
}
