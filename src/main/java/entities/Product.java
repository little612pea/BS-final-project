package entities;

import java.util.Comparator;
import java.util.Objects;
import java.util.Random;

public final class Product {
    private int productId; //->ID
    private String comment;  //->COMMENT
    private String title; //->TITLE
    private String shop; //->SHOP
    private String deal; //->DEAL
    private String img_url; //->IMG_URL
    private double price; //->PRICE
    private int source; //->SOURCE 0 淘宝 1 京东

    public enum SortColumn {
        PRODUCT_ID("productId", Comparator.comparingInt(Product::getProductId)),
        CATEGORY("comment", Comparator.comparing(Product::getComment)),
        TITLE("title", Comparator.comparing(Product::getTitle)),
        PRESS("shop", Comparator.comparing(Product::getShop)),
        PUBLISH_YEAR("deal", Comparator.comparing(Product::getDeal)),
        AUTHOR("img_url", Comparator.comparing(Product::getImg)),
        PRICE("price", Comparator.comparingDouble(Product::getPrice)),
        STOCK("source", Comparator.comparingInt(Product::getSource));

        private final String value;
        private final Comparator<Product> comparator;

        public String getValue() {
            return value;
        }

        public Comparator<Product> getComparator() {
            return comparator;
        }

        SortColumn(String value, Comparator<Product> comparator) {
            this.value = value;
            this.comparator = comparator;
        }

        public static SortColumn random() {
            return values()[new Random().nextInt(values().length)];
        }
    }

    public Product() {
    }

    public Product(String comment, String title, String shop, String deal,
                String img_url, double price, int source) {
        this.comment = comment;
        this.title = title;
        this.shop = shop;
        this.deal = deal;
        this.img_url = img_url;
        this.price = price;
        this.source = source;
    }

    @Override
    public Product clone() {
        Product b = new Product(comment, title, shop, deal, img_url, price, source);
        b.productId = productId;
        return b;
    }

    @Override
    public String toString() {
        return "Product {" + "productId=" + productId +
                ", comment='" + comment + '\'' +
                ", title='" + title + '\'' +
                ", shop='" + shop + '\'' +
                ", deal=" + deal +
                ", img_url='" + img_url + '\'' +
                ", price=" + String.format("%.2f", price) +
                ", source=" + source +
                '}';
    }

    /* we assume that two products are equal iff their comment...img_url are equal */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return deal == product.deal &&
                comment.equals(product.comment) &&
                title.equals(product.title) &&
                shop.equals(product.shop) &&
                img_url.equals(product.img_url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment, title, shop, deal, img_url);
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        this.shop = shop;
    }

    public String getDeal() {
        return deal;
    }

    public void setDeal(String deal) {
        this.deal = deal;
    }

    public String getImg() {
        return img_url;
    }

    public void setImg(String img_url) {
        this.img_url = img_url;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }
}
