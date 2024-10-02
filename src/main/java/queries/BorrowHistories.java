package queries;

import entities.Product;
import entities.Borrow;

import java.util.List;

public class BorrowHistories {

    public static class Item {
        private int cardId;
        private int productId;
        private String comment;
        private String title;
        private String shop;
        private String deal;
        private String img_url;
        private double price;
        private long borrowTime;
        private long returnTime;

        public Item() {
        }

        public Item(int cardId, Product product, Borrow borrow) {
            this.cardId = cardId;
            this.productId = product.getProductId();
            this.comment = product.getComment();
            this.title = product.getTitle();
            this.shop = product.getShop();
            this.deal = product.getDeal();
            this.img_url = product.getImg();
            this.price = product.getPrice();
            this.borrowTime = borrow.getBorrowTime();
            this.returnTime = borrow.getReturnTime();
        }

        @Override
        public String toString() {
            return "Item {" + "cardId=" + cardId +
                    ", productId=" + productId +
                    ", comment='" + comment + '\'' +
                    ", title='" + title + '\'' +
                    ", shop='" + shop + '\'' +
                    ", deal=" + deal +
                    ", img_url='" + img_url + '\'' +
                    ", price=" + price +
                    ", borrowTime=" + borrowTime +
                    ", returnTime=" + returnTime +
                    '}';
        }

        public int getCardId() {
            return cardId;
        }

        public void setCardId(int cardId) {
            this.cardId = cardId;
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

        public long getBorrowTime() {
            return borrowTime;
        }

        public void setBorrowTime(long borrowTime) {
            this.borrowTime = borrowTime;
        }

        public long getReturnTime() {
            return returnTime;
        }

        public void setReturnTime(long returnTime) {
            this.returnTime = returnTime;
        }
    }

    private int count;
    private List<Item> items;

    public BorrowHistories(List<Item> items) {
        this.count = items.size();
        this.items = items;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
