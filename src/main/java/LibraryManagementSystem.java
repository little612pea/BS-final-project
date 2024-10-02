import entities.Product;
import entities.Borrow;
import entities.Card;
import queries.ApiResult;
import queries.ProductQueryConditions;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Note:
 *      (1) all functions in this interface will be regarded as a
 *          transaction. this means that after successfully completing
 *          all operations in a function, you need to call commit(),
 *          or call rollback() if one of the operations in a function fails.
 *          as an example, you can see {@link LibraryManagementSystemImpl#resetDatabase}
 *          to find how to use commit() and rollback().
 *      (2) for each function, you need to briefly introduce how to
 *          achieve this function and how to solve challenges in your
 *          lab report.
 *      (3) if you don't know what the function means, or what it is
 *          supposed to do, looking to the test code might help.
 */
public interface LibraryManagementSystem {

    /* Interface for products */

    /**
     * register a product to database.
     *
     * Note that:
     *      (1) productId should be stored to product after successfully
     *          completing this operation.
     *      (2) you should not register this product if the product already
     *          exists in the library system.
     *
     * @param product all attributes of the product
     */
    ApiResult storeProduct(Product product);

    /**
     * increase the product's inventory by productId & deltaStock.
     *
     * Note that:
     *      (1) you need to check the correctness of productId
     *      (2) deltaStock can be negative, but make sure that
     *          the result of product.source + deltaStock is not negative!
     *
     * @param productId product's productId
     * @param deltaStock increase count to product's source, must be greater
     */
    ApiResult incProductStock(int productId, int deltaStock);

    /**
     * batch store products.
     *
     * Note that:
     *      (1) you should not call the interface storeProduct()
     *          multiple times to achieve this function!!!
     *          hint: use {@link PreparedStatement#executeBatch()}
     *          and {@link PreparedStatement#addBatch()}
     *      (2) if one of the products fails to import, all operations
     *          should be rolled back using rollback() function provided
     *          by JDBC!!!
     *      (3) when binding params to SQL, you are required to avoid
     *          the risk of SQL injection attack!!!
     *
     * @param products list of products to be stored
     */
    ApiResult storeProduct(List<Product> products);

    /**
     * remove this product from library system.
     *
     * Note that if someone has not returned this product,
     * the product should not be removed!
     *
     * @param productId the product to be removed
     */
    ApiResult removeProduct(int productId);

    /**
     * modify a product's information by productId.productId.
     *
     * Note that you should not modify its productId and source!
     *
     * @param product the product to be modified
     */
    ApiResult modifyProductInfo(Product product);

    /**
     * query products according to different query conditions.
     *
     * Note that:
     *      (1) you should let the DBMS to filter records
     *          that do not satisfy the conditions instead of
     *          filter records in your API.
     *      (2) when binding params to SQL, you also need to avoid
     *          the risk of SQL injection attack.
     *      (3) [*] if all else is equal, sort by productId in
     *          ascending order!
     *
     * @param conditions query conditions
     *
     * @return query results should be returned by ApiResult.payload
     *         and should be an instance of {@link queries.ProductQueryResults}
     */
    ApiResult queryProduct(ProductQueryConditions conditions);

    /* Interface for borrow & return products */

    /**
     * a user borrows one product with the specific card.
     * the borrow operation will success iff there are
     * enough products in source & the user has not borrowed
     * the product or has returned it.
     *
     * @param borrow borrow information, include borrower &
     *               product's id & time
     */
    ApiResult borrowProduct(Borrow borrow);

    /**
     * A user return one product with specific card.
     *
     * @param borrow borrow information, include borrower & product's id & return time
     */
    ApiResult returnProduct(Borrow borrow);

    /**
     * list all borrow histories for a specific card.
     * the returned records should be sorted by borrow_time DESC, productId ASC
     *
     * @param cardId show which card's borrow history
     * @return query results should be returned by ApiResult.payload
     *         and should be an instance of {@link queries.BorrowHistories}
     */
    ApiResult showBorrowHistory(int cardId);

    /**
     * create a new borrow card. do nothing and return failed if
     * the card already exists.
     *
     * Note that card_id should be stored to card after successfully
     * completing this operation.
     *
     * @param card all attributes of the card
     */
    ApiResult registerCard(Card card);

    /**
     * simply remove a card.
     *
     * Note that if there exists any un-returned products under this user,
     * this card should not be removed.
     *
     * @param cardId card to be removed
     */
    ApiResult removeCard(int cardId);

    /**
     * list all cards order by card_id.
     *
     * @return query results should be returned by ApiResult.payload
     *         and should be an instance of {@link queries.CardList}
     */
    ApiResult showCards();

    ApiResult ModifyCard(int cardId, String name, String department, String type);

    /**
     * reset database to its initial state.
     * you are not allowed to complete & modify this function.
     */
    ApiResult resetDatabase();
    ApiResult login(String username, String password);
    ApiResult register(String username, String password, String email);
}
