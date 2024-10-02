package queries;

import entities.Product;

import java.util.List;

public class ProductQueryResults {

    private int count;   /* number of results, equal to results.size() */
    private List<Product> results;

    public ProductQueryResults(List<Product> results) {
        this.count = results.size();
        this.results = results;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Product> getResults() {
        return results;
    }

    public void setResults(List<Product> results) {
        this.results = results;
    }
}
