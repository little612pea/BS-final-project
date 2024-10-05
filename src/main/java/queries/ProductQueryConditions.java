package queries;

import entities.Product;

/**
 * Note: (1) all non-null attributes should be used as query
 *       conditions and connected by "AND" operations.
 *       (2) for range query of an attribute, the maximum and
 *       minimum values use closed intervals.
 *       eg: minA=x, maxA=y ==> x <= A <= y
 *           minA=null, maxA=y ==> A <= y
 *           minA=x, maxA=null ==> A >= x
 * */
public class ProductQueryConditions {
    /* Note: use exact matching */
    private String comment;
    /* Note: use fuzzy matching */
    private String title;
    /* Note: use fuzzy matching */
    private String shop;
    private Integer minPublishYear;
    private Integer maxPublishYear;
    /* Note: use fuzzy matching */
    private String img_url;
    private Double minPrice;
    private Double maxPrice;
    /* sort by which field */
    private Product.SortColumn sortBy;
    /* default sort by PK */
    private SortOrder sortOrder;

    public ProductQueryConditions() {
        this.comment = null;
        this.title = null;
        this.shop = null;
        this.minPublishYear = null;
        this.maxPublishYear = null;
        this.img_url = null;
        this.minPrice = null;
        this.maxPrice = null;
        sortBy = Product.SortColumn.ProductId;
        sortOrder = SortOrder.ASC;
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

    public Integer getMinPublishYear() {
        return minPublishYear;
    }

    public void setMinPublishYear(Integer minPublishYear) {
        this.minPublishYear = minPublishYear;
    }

    public Integer getMaxPublishYear() {
        return maxPublishYear;
    }

    public void setMaxPublishYear(Integer maxPublishYear) {
        this.maxPublishYear = maxPublishYear;
    }

    public String getImg() {
        return img_url;
    }

    public void setImg(String img_url) {
        this.img_url = img_url;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Product.SortColumn getSortBy() {
        return sortBy;
    }

    public void setSortBy(Product.SortColumn sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
