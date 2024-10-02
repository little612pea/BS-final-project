import entities.Product;
import entities.Borrow;
import entities.Card;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import queries.ApiResult;
import utils.RandomData;

import java.util.*;

public class MyLibrary {

    public List<Product> products;
    public List<Card> cards;
    public List<Borrow> borrows;

    public MyLibrary(List<Product> products, List<Card> cards, List<Borrow> borrows) {
        this.products = products;
        this.cards = cards;
        this.borrows = borrows;
    }

    public int nProducts() {
        return products.size();
    }

    public int nCards() {
        return cards.size();
    }

    public int nBorrows() {
        return borrows.size();
    }

    public static MyLibrary createLibrary(LibraryManagementSystem library, int nProducts,
                                          int nCards, int nBorrows) {
        /* create products */
        Set<Product> productSet = new HashSet<>();
        while (productSet.size() < nProducts) {
            productSet.add(RandomData.randomProduct());
        }
        List<Product> productList = new ArrayList<>(productSet);
        ApiResult res = library.storeProduct(productList);
        Assert.assertTrue(res.ok);
        /* create cards */
        List<Card> cardList = new ArrayList<>();
        for (int i = 0; i < nCards; i++) {
            Card c = new Card();
            c.setName(String.format("User%05d", i));
            c.setDepartment(RandomData.randomDepartment());
            c.setType(Card.CardType.random());
            cardList.add(c);
            Assert.assertTrue(library.registerCard(c).ok);
        }
        /* create histories */
        List<Borrow> borrowList = new ArrayList<>();
        PriorityQueue<Long> mills = new PriorityQueue<>();
        for (int i = 0; i < nBorrows * 2; i++) {
            mills.add(RandomData.randomTime());
        }
        for (int i = 0; i < nBorrows;) {
            Product b = productList.get(RandomUtils.nextInt(0, nProducts));
            if (b.getSource() == 0) {
                continue;
            }
            i++;
            Card c = cardList.get(RandomUtils.nextInt(0, nCards));
            Borrow r = new Borrow();
            r.setCardId(c.getCardId());
            r.setProductId(b.getProductId());
            r.setBorrowTime(mills.poll());
            r.setReturnTime(mills.poll());
            System.out.printf("%d\n",i);
            System.out.print(r);
            Assert.assertTrue(library.borrowProduct(r).ok);
            Assert.assertTrue(library.returnProduct(r).ok);
            borrowList.add(r);
        }
        return new MyLibrary(productList, cardList, borrowList);
    }

}
