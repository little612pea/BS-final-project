package database;

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
    ApiResult storeProduct(String username,Product product);

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
    ApiResult storeProduct(String username,List<Product> products);


    /**
     * modify a product's information by productId.productId.
     *
     * Note that you should not modify its productId and source!
     *
     * @param product the product to be modified
     */
    ApiResult modifyLikeStatus(String user_name,Product product);
    public ApiResult getAllUsernames();
    public ApiResult getUserFavoriteProducts(String username);
    public ApiResult modifyPrice(String user_name,Product product);

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
    ApiResult queryProduct(String user_name, ProductQueryConditions conditions);


    /**
     * reset database to its initial state.
     * you are not allowed to complete & modify this function.
     */
    ApiResult resetDatabase();
    ApiResult login(String username, String password);
    ApiResult register(String username, String password, String email);
    ApiResult searchEmail(String username);
    ApiResult createUserTable(String username);
}
