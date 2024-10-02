package utils;

public interface DBInitializer {

    String sqlDropProduct();
    String sqlDropCard();
    String sqlDropBorrow();
    String sqlCreateProduct();
    String sqlCreateCard();
    String sqlCreateBorrow();

}
