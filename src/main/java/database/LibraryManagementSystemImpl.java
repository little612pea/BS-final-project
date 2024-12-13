package database;

import com.google.protobuf.Api;
import entities.Product;
import entities.Borrow;
import entities.Card;
import javafx.util.Pair;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.util.Objects;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import queries.ApiResult;
import java.util.Comparator;
import queries.ProductQueryConditions;
import queries.ProductQueryResults;
import queries.SortOrder;


public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }


    public ApiResult login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new ApiResult(true, "Login successful");
            } else {
                return new ApiResult(false, "Invalid username or password");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return new ApiResult(false, "Database error");
        }
    }

    public ApiResult createAccountTable(){
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (\n"
                + "    id INT AUTO_INCREMENT PRIMARY KEY,\n"
                + "    username VARCHAR(255) UNIQUE NOT NULL,\n"
                + "    password VARCHAR(255) NOT NULL,\n"
                + "    email VARCHAR(255) UNIQUE NOT NULL\n"
                + ")engine=innodb charset=utf8mb4";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(createTableSQL)) {
            // 执行创建表的 SQL
            statement.executeUpdate();
            System.out.println("Table users created successfully.");
            return new ApiResult(true, "Table users created successfully.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return new ApiResult(false, "Failed to create table users");
        }
    }
    public ApiResult createUserTable(String username) {
        // 动态拼接表名
        String tableName = "product_" + username;
        ApiResult accountTable = createAccountTable();
        // SQL 语句：创建表（如果不存在）
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n"
                + "    productId INT AUTO_INCREMENT PRIMARY KEY,\n"
                + "    comment varchar(255) null,\n"
                + "    title VARCHAR(255) NOT NULL,\n"
                + "    shop VARCHAR(255) NOT NULL,\n"
                + "    deal VARCHAR(255),\n"
                + "    img_url VARCHAR(1000),\n"
                + "    price DOUBLE NOT NULL,\n"
                + "    source VARCHAR(1000) NOT NULL,\n"
                + "    favorite INT DEFAULT 0\n"
                + ")engine=innodb charset=utf8mb4";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(createTableSQL)) {
            // 执行创建表的 SQL
            statement.executeUpdate();
            return new ApiResult(true, "Table " + tableName + " created successfully.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return new ApiResult(false, "Failed to create table for username " + username);
        }
    }

    public ApiResult register(String username, String password, String email) {
        // 检查用户名是否已存在
        createUserTable(username);
        String checkSql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(checkSql)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new ApiResult(false, "Username already exists");
            }
        } catch (SQLException e) {
            System.out.println("Database error during check: " + e.getMessage());
            return new ApiResult(false, "Database error");
        }

        // 插入新用户
        String insertSql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(insertSql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, email);
            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                commit(connector.getConn());
                return new ApiResult(true, "User registered successfully");
            } else {
                return new ApiResult(false, "Registration failed, no rows affected");
            }
        } catch (SQLException e) {
            System.out.println("Database error during insert: " + e.getMessage());
            return new ApiResult(false, "Database error");
        }
    }
    public ApiResult searchEmail(String username) {
        // 检查用户是否存在
        String checkSql = "SELECT email FROM users WHERE username = ?";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(checkSql)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String email = resultSet.getString("email");
                return new ApiResult(true, email);  // 返回查询到的email
            } else {
                return new ApiResult(false, "Username not found");
            }
        } catch (SQLException e) {
            System.out.println("Database error during search: " + e.getMessage());
            return new ApiResult(false, "Database error");
        }
    }
    public ApiResult getAllUsernames() {
        String sql = "SELECT username FROM users"; // 假设用户表中包含 username 字段
        List<String> usernameList = new ArrayList<>();
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                usernameList.add(resultSet.getString("username"));
            }
            return new ApiResult(true, usernameList);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return new ApiResult(false, "Database error");
        }
    }

    @Override
    public ApiResult storeProduct(String userName, Product product) {
        String tableName = "product_" + userName;

        // 查询总数 SQL
        String countSQL = "SELECT COUNT(*) FROM " + tableName;

        // 删除最旧的 500 条记录 SQL
        String deleteSQL = "DELETE FROM " + tableName + " ORDER BY productId ASC LIMIT 500";

        // 插入新记录 SQL
        String insertSQL = "INSERT INTO " + tableName + " VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement countStatement = this.connector.getConn().prepareStatement(countSQL);
             PreparedStatement deleteStatement = this.connector.getConn().prepareStatement(deleteSQL);) {
            // 设置参数
            ResultSet countResult = countStatement.executeQuery();
            int recordCount = 0;
            if (countResult.next()) {
                recordCount = countResult.getInt(1);
            }
            if (recordCount > 2000) {
                deleteStatement.executeUpdate();
            }
            if (product.getComment() != null) {
                statement.setString(1, product.getComment());
            } else {
                statement.setNull(1, Types.VARCHAR);  // 设置为 null
            }

            statement.setString(2, product.getTitle());

            statement.setString(3, product.getShop());

            if (product.getDeal() != null) {
                statement.setString(4, product.getDeal());
            } else {
                statement.setNull(4, Types.VARCHAR);  // 设置为 null
            }

            statement.setString(5, product.getImg());
            statement.setDouble(6, product.getPrice());
            statement.setString(7,product.getSource());
            statement.setInt(8,product.getFavorite());
            // 执行插入
            if(isProductDuplicate(userName,product)){
                System.out.println("duplicate products");
                return new ApiResult(false,"duplicate products,no rows affected.");
            }
            else{
                int affectedRows = statement.executeUpdate();
                if (affectedRows > 0) {
                    // 获取生成的主键
                    try (ResultSet rs = statement.getGeneratedKeys()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            //System.out.println(id);
                            product.setProductId(id);
                            commit(connector.getConn());
                            return new ApiResult(true,id); // 获取生成的 productId

                        } else {
                            System.out.println("Creating user failed, no ID obtained.");
                            return new ApiResult(false,"Creating user failed, no ID obtained.");
                        }
                    }
                } else {
                    //System.out.println("hereee 2");
                    return new ApiResult(false,"Creating user failed, no rows affected.");
                }
            }
        }
        catch(Exception e){
            System.out.println("exception:"+e);
            return new ApiResult(false, "Unimplemented Function");
        }

    }

    private boolean isProductDuplicate(String user_name,Product product) throws SQLException {
        Connection conn = this.connector.getConn();
        String tableName = "product_" + user_name;
        String checkSql = "SELECT * FROM " + tableName + " WHERE comment = ? AND title = ? AND shop = ? AND deal = ? AND img_url = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, product.getComment());
            stmt.setString(2, product.getTitle());
            stmt.setString(3, product.getShop());
            stmt.setString(4, product.getDeal());
            stmt.setString(5, product.getImg());
            try (ResultSet rs = stmt.executeQuery()) {
                // 如果查询有结果，则表示书籍重复
                return rs.next(); // 只需要检查是否有至少一行数据，因为我们已经通过所有关键列进行了查询
            }
        }
    }

    @Override
    public ApiResult storeProduct(String username,List<Product> products) {
        Connection conn = this.connector.getConn();
        try {
            // 开始事务
            conn.setAutoCommit(false);
            for (Product product : products) {
                ApiResult result = storeProduct(username,product);
                // 如果任何一本书入库失败，则中断循环并回滚事务
                if (!result.ok) {
                    conn.rollback(); // 回滚事务
                    return result; // 返回失败的ApiResult
                }
            }
            // 如果所有书籍都成功入库，则提交事务
            commit(connector.getConn());
            return new ApiResult(true, "All products stored successfully.");
        } catch (SQLException e) {
            // 处理数据库异常，回滚事务
            System.out.println("Error occurred while storing products: " + e.getMessage());
            return new ApiResult(false, "Database error occurred: " + e.getMessage());
        }
    }

    @Override
    public ApiResult modifyLikeStatus(String user_name,Product product) {
        String tableName = "product_" + user_name;
        String sql = "UPDATE " + tableName + " SET favorite = ? WHERE productId = ?";
        try(PreparedStatement ps = connector.getConn().prepareStatement(sql)){
            int favor=0;
            if(product.getFavorite()==0) favor = 0;
            else if (product.getFavorite()==1) favor = 1;
            System.out.println("favor:"+favor);
            ps.setInt(1,favor);
            ps.setInt(2,product.getProductId());
            if(ps.executeUpdate()==0){
                System.out.println("fail here 1");
                return new ApiResult(false, "fail");
            }

        }
        catch(SQLException e ){
            System.out.println("fail here 2"+e);
            return new ApiResult(false, "fail");
        }

        commit(connector.getConn());
        System.out.println("success!");
        return new ApiResult(true, "success");
    }

    @Override
    public ApiResult modifyPrice(String user_name,Product product) {
        String tableName = "product_" + user_name;
        try(PreparedStatement ps = connector.getConn().prepareStatement("UPDATE " + tableName + " SET price = ? WHERE productId = ?")){
            ps.setDouble(1,product.getPrice());
            ps.setInt(2,product.getProductId());
            if(ps.executeUpdate()==0){
                System.out.println("fail here 1");
                return new ApiResult(false, "fail");
            }

        }
        catch(SQLException e ){
            System.out.println("fail here 2"+e);
            return new ApiResult(false, "fail");
        }

        commit(connector.getConn());
        System.out.println("success!");
        return new ApiResult(true, "success");
    }

    public ApiResult getUserFavoriteProducts(String username) {
        String tableName = "product_" + username;
        String sql = "SELECT * FROM " + tableName + " WHERE favorite = 1";
        List<Product> productList = new ArrayList<>();
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Product product = new Product();
                product.setProductId(resultSet.getInt("productId"));
                product.setComment(resultSet.getString("comment"));
                product.setTitle(resultSet.getString("title"));
                product.setShop(resultSet.getString("shop"));
                product.setDeal(resultSet.getString("deal"));
                product.setImg(resultSet.getString("img_url"));
                product.setPrice(resultSet.getDouble("price"));
                product.setSource(resultSet.getString("source"));
                product.setFavorite(resultSet.getInt("favorite"));
                productList.add(product);
            }
            return new ApiResult(true, productList);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return new ApiResult(false, "Database error");
        }
    }
    @Override
    public ApiResult queryProduct(String user_name,ProductQueryConditions conditions) {
        String tableName = "product_" + user_name;
        String query = "SELECT * FROM " + tableName;
        //String whereClause = buildWhereClause(conditions);
        Pair<String, List<Object>> whereClauseAndParameters = buildWhereClause(conditions);
        String whereClause = (String) whereClauseAndParameters.getKey();
        String orderByClause = buildOrderByClause(conditions);
        String s= query + whereClause + orderByClause;

        try (PreparedStatement ps = connector.getConn().prepareStatement(s)) {
            int parameterIndex = 1; // JDBC parameters are 1-indexed
            for (Object parameter : whereClauseAndParameters.getValue()) {
                System.out.println("here is the parameter:"+parameter);
                ps.setObject(parameterIndex++, parameter);
            }

            System.out.println("here is the sql query:"+ps);
            ResultSet rs = ps.executeQuery();
            List<Product> ResultList = new ArrayList<Product>();

            while (rs.next()) {
                Product newproduct = new Product();
                newproduct.setProductId(rs.getInt("productId"));
                newproduct.setComment(rs.getString("comment"));
                newproduct.setTitle(rs.getString("title"));
                newproduct.setShop(rs.getString("shop"));
                newproduct.setDeal(rs.getString("deal"));
                newproduct.setImg(rs.getString("img_url"));
                newproduct.setPrice(rs.getDouble("price"));
                newproduct.setSource(rs.getString("source"));
                newproduct.setFavorite(rs.getInt("favorite"));
                ResultList.add(newproduct);

            }
            Comparator<Product> cmp = conditions.getSortBy().getComparator();
            if (conditions.getSortOrder() == SortOrder.DESC) {
                cmp = cmp.reversed();
            }
            Comparator<Product> comparator = cmp;
            Comparator<Product> sortComparator = cmp.thenComparingInt(Product::getProductId);
            ResultList.sort(sortComparator);
            ProductQueryResults Results = new ProductQueryResults(ResultList);
            commit(connector.getConn());
            return new ApiResult(true, "success ", Results);
        } catch (SQLException e) {
            System.out.println("Error occurred while querying products: " + e.getMessage());
            return new ApiResult(false,"error!");
        }
    }
    private Pair buildWhereClause(ProductQueryConditions conditions) {
        StringBuilder where = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        where.append(" WHERE 1=1");
        if (conditions.getComment() != null) {
            where.append(" AND comment = ?");
            parameters.add(conditions.getComment());
        }
        if (conditions.getTitle() != null) {
            where.append(" AND title LIKE ?");
            parameters.add("%" + conditions.getTitle() + "%");
        }
        if (conditions.getShop() != null) {
            where.append(" AND shop LIKE ?");
            parameters.add("%" + conditions.getShop() + "%");
        }
        if (conditions.getMinPublishYear() != null) {
            where.append(" AND deal >= ?");
            parameters.add(conditions.getMinPublishYear());
        }
        if (conditions.getMaxPublishYear() != null) {
            where.append(" AND deal <= ?");
            parameters.add(conditions.getMaxPublishYear());
        }
        if (conditions.getImg() != null) {
            where.append(" AND img_url LIKE ?");
            parameters.add("%" + conditions.getImg() + "%");
        }
        if (conditions.getMinPrice() != null) {
            where.append(" AND price >= ?");
            parameters.add(conditions.getMinPrice());
        }
        if (conditions.getMaxPrice() != null) {
            where.append(" AND price <= ?");
            parameters.add(conditions.getMaxPrice());
        }
        // 返回带有占位符的SQL语句和对应的参数列表
        return new Pair<>(where.toString(), parameters);
    }
    private String buildOrderByClause(ProductQueryConditions conditions) {
        return " ORDER BY " + conditions.getSortBy() + " " + conditions.getSortOrder();
    }


    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropProduct());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateProduct());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback(); // 回滚事务
            } catch (SQLException rollbackError) {
                rollbackError.printStackTrace();
            }
            e.printStackTrace();
        }
    }


}
