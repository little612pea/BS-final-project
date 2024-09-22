import entities.Book;
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
import queries.BookQueryConditions;
import queries.BookQueryResults;
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

    public ApiResult register(String username, String password) {
        // 检查用户名是否已存在
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
        String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(insertSql)) {
            statement.setString(1, username);
            statement.setString(2, password);
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
    @Override
    public ApiResult storeBook(Book book) {
        String insertSQL = "INSERT INTO book VALUES (null, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            // 设置参数
            statement.setString(1, book.getCategory());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getPress());
            statement.setInt(4, book.getPublishYear());
            statement.setString(5, book.getAuthor());
            statement.setDouble(6,book.getPrice());
            statement.setInt(7,book.getStock());
            // 执行插入
            if(isBookDuplicate(book)){
                System.out.println("duplicate books");
                return new ApiResult(false,"duplicate books,no rows affected.");
            }
            else{
                int affectedRows = statement.executeUpdate();
                if (affectedRows > 0) {
                    // 获取生成的主键
                    try (ResultSet rs = statement.getGeneratedKeys()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            //System.out.println(id);
                            book.setBookId(id);
                            commit(connector.getConn());
                            return new ApiResult(true,id); // 获取生成的 book_id

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
            System.out.println("Unimplemented Function！");
            return new ApiResult(false, "Unimplemented Function");
        }

    }

    private boolean isBookDuplicate(Book book) throws SQLException {
        Connection conn = this.connector.getConn();
        String checkSql = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, book.getCategory());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getPress());
            stmt.setInt(4, book.getPublishYear());
            stmt.setString(5, book.getAuthor());
            try (ResultSet rs = stmt.executeQuery()) {
                // 如果查询有结果，则表示书籍重复
                return rs.next(); // 只需要检查是否有至少一行数据，因为我们已经通过所有关键列进行了查询
            }
        }
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        try(PreparedStatement ps=connector.getConn().prepareStatement("select * from book where book_id=?" )){
            ps.setInt(1,bookId);
            ResultSet rs=ps.executeQuery();
            if(!rs.next()){
                return new ApiResult(false, "fail");
            }
            else{
                if(rs.getInt("stock")+deltaStock<0){
                    System.out.println("stock is not enough");
                    return new ApiResult(false, "fail");
                }
                else{
                    try(PreparedStatement ps1=connector.getConn().prepareStatement("update book set stock=stock+? where book_id=?" )){
                        ps1.setInt(1,deltaStock);
                        ps1.setInt(2,bookId);

                        if(ps1.executeUpdate()>0){
                            commit(connector.getConn());
                            //System.out.println("success");
                            return new ApiResult(true, "success");
                        }
                    }
                }
            }
        }
        catch(SQLException e ){
            e.printStackTrace();
        }
        return new ApiResult(false, "fail");
    }


    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = this.connector.getConn();
        int flag=0;
        for(int i=0;i<books.size();i++){
            for(int j=i+1;j<books.size();j++){
                if (books.get(i).equals(books.get(j))) {
                    flag = 1;
                    break;
                }
            }
        }
        if(flag==1) return new ApiResult(false, "fail");
        try {
            // 开始事务
            conn.setAutoCommit(false);
            for (Book book : books) {
                ApiResult result = storeBook(book);
                // 如果任何一本书入库失败，则中断循环并回滚事务
                if (!result.ok) {
                    conn.rollback(); // 回滚事务
                    return result; // 返回失败的ApiResult
                }
            }
            // 如果所有书籍都成功入库，则提交事务
            commit(connector.getConn());
            return new ApiResult(true, "All books stored successfully.");
        } catch (SQLException e) {
            // 处理数据库异常，回滚事务
            System.out.println("Error occurred while storing books: " + e.getMessage());
            return new ApiResult(false, "Database error occurred: " + e.getMessage());
        }
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = this.connector.getConn();

        String select_stock_sql = "SELECT * FROM borrow WHERE book_id = ?"; // 查询是否有未归还的书
        String delete_book_sql = "DELETE FROM book WHERE book_id = ?";
        try (PreparedStatement ps = connector.getConn().prepareStatement("select * from book where book_id = ?")) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            if(!rs.next()) {
                return new ApiResult(false, "fail");
            }
        } catch (SQLException e) {
            return new ApiResult(false, "fail");
        }
        try (PreparedStatement stmt = conn.prepareStatement(select_stock_sql)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                if (rs.getLong("return_time") == 0) {
                    return new ApiResult(false, "无法删除，因为这本书还有人尚未归还。");}
            }
        }
        catch (SQLException e) {
            return new ApiResult(false, "删除书籍时出现错误: " + e.getMessage());
        }

        try (PreparedStatement dstmt = conn.prepareStatement(delete_book_sql)) {
            dstmt.setInt(1, bookId);
            int rowsAffected = dstmt.executeUpdate();

            if (rowsAffected > 0) {
                return new ApiResult(true, "成功从仓库中删除书籍");
            } else {
                return new ApiResult(false, "无法删除书籍，可能不存在该书籍记录");
            }
        } catch (SQLException e) {
            return new ApiResult(false, "删除书籍时出现错误: " + e.getMessage());
        }
    }
    @Override
    public ApiResult modifyBookInfo(Book book) {
        try(PreparedStatement ps = connector.getConn().prepareStatement("update book set category=?,title=?,press=?,publish_year=?,author=?,price=? where book_id=?")){
            ps.setString(1,book.getCategory());
            ps.setString(2,book.getTitle());
            ps.setString(3,book.getPress());
            ps.setInt(4,book.getPublishYear());
            ps.setString(5,book.getAuthor());
            ps.setDouble(6,book.getPrice());
            ps.setInt(7,book.getBookId());
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
    public ApiResult queryBook(BookQueryConditions conditions) {
        String query = "SELECT * FROM book";
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
            List<Book> ResultList = new ArrayList<Book>();

            while (rs.next()) {
                Book newbook = new Book();
                newbook.setBookId(rs.getInt("book_id"));
                newbook.setCategory(rs.getString("category"));
                newbook.setTitle(rs.getString("title"));
                newbook.setPress(rs.getString("press"));
                newbook.setPublishYear(rs.getInt("publish_year"));
                newbook.setAuthor(rs.getString("author"));
                newbook.setPrice(rs.getDouble("price"));
                newbook.setStock(rs.getInt("stock"));
                ResultList.add(newbook);

            }
            Comparator<Book> cmp = conditions.getSortBy().getComparator();
            if (conditions.getSortOrder() == SortOrder.DESC) {
                cmp = cmp.reversed();
            }
            Comparator<Book> comparator = cmp;
            Comparator<Book> sortComparator = cmp.thenComparingInt(Book::getBookId);
            ResultList.sort(sortComparator);
            BookQueryResults Results = new BookQueryResults(ResultList);
            commit(connector.getConn());
            return new ApiResult(true, "success ", Results);
        } catch (SQLException e) {
            return new ApiResult(false,"error!");
        }
    }
    private Pair buildWhereClause(BookQueryConditions conditions) {
        StringBuilder where = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        where.append(" WHERE 1=1");
        if (conditions.getCategory() != null) {
            where.append(" AND category = ?");
            parameters.add(conditions.getCategory());
        }
        if (conditions.getTitle() != null) {
            where.append(" AND title LIKE ?");
            parameters.add("%" + conditions.getTitle() + "%");
        }
        if (conditions.getPress() != null) {
            where.append(" AND press LIKE ?");
            parameters.add("%" + conditions.getPress() + "%");
        }
        if (conditions.getMinPublishYear() != null) {
            where.append(" AND publish_year >= ?");
            parameters.add(conditions.getMinPublishYear());
        }
        if (conditions.getMaxPublishYear() != null) {
            where.append(" AND publish_year <= ?");
            parameters.add(conditions.getMaxPublishYear());
        }
        if (conditions.getAuthor() != null) {
            where.append(" AND author LIKE ?");
            parameters.add("%" + conditions.getAuthor() + "%");
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
    private String buildOrderByClause(BookQueryConditions conditions) {
        return " ORDER BY " + conditions.getSortBy() + " " + conditions.getSortOrder();
    }
    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            //conn.setAutoCommit(false);
            // 检查借书卡是否存在并锁定
            try (PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM card WHERE card_id = ? FOR UPDATE")) {
                ps2.setInt(1, borrow.getCardId());
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (!rs2.next()) {
                        System.out.println("The card does not exist");
                        return new ApiResult(false, "The card does not exist");

                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                conn.rollback();
                System.out.println("Error occurred while checking the card");
                return new ApiResult(false, "Error occurred while checking the card");
            }


            String lookfor_sql = "SELECT * FROM borrow WHERE card_id=? AND book_id=?";
            try (PreparedStatement stmt = conn.prepareStatement(lookfor_sql)) {
                stmt.setInt(1, borrow.getCardId());
                stmt.setInt(2, borrow.getBookId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while(rs.next()){
                        if(rs.getLong("return_time")==0){
                            conn.rollback();
                            System.out.println("you have already rented this book");
                            return new ApiResult(false,"you have already rented this book");
                        }
                    }
                }
                catch (Exception e){
                    System.out.println("error1");
                    return new ApiResult(false,"error");
                }
            }
            catch (Exception e){
                System.out.println("error2");
                return new ApiResult(false,"error");
            }

            // 检查书是否存在并且库存充足并锁定
            try (PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM book WHERE book_id = ? FOR UPDATE")) {
                ps1.setInt(1, borrow.getBookId());
                try (ResultSet rs1 = ps1.executeQuery()) {
                    if (!rs1.next()) {
                        System.out.println("The book does not exist");
                        return new ApiResult(false, "The book does not exist");
                    }
                    if (rs1.getInt("stock") == 0) {
                        System.out.println("The book is out of stock");
                        return new ApiResult(false, "The book is out of stock");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                conn.rollback();
                System.out.println("Error occurred while checking the book");
                return new ApiResult(false, "Error occurred while checking the book");
            }

            // 借书操作
            String borrow_sql = "INSERT INTO borrow VALUES (?, ?, ?, ?)";
            try (PreparedStatement bstmt = conn.prepareStatement(borrow_sql)) {
                bstmt.setInt(1, borrow.getCardId());
                bstmt.setInt(2, borrow.getBookId());
                bstmt.setLong(3, borrow.getBorrowTime());
                bstmt.setLong(4, 0);
                //commit(connector.getConn());
                if (bstmt.executeUpdate() > 0) {
                    // 更新书的库存
                    try (PreparedStatement ps4 = conn.prepareStatement("UPDATE book SET stock = stock - 1 WHERE book_id = ?")) {
                        ps4.setInt(1, borrow.getBookId());
                        ps4.executeUpdate();
                    }
                    //conn.commit();
                    commit(connector.getConn());
                    //System.out.println("Success");
                    return new ApiResult(true, "Success");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                conn.rollback();
                System.out.println("Error occurred while borrowing the book");
                return new ApiResult(false, "Error occurred while borrowing the book");
            }
            System.out.println("Error 3");
            return new ApiResult(false, "Error occurred");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error 4");
            return new ApiResult(false, "Error occurred");
        } finally {
            commit(connector.getConn());
        }
    }


    @Override
    public ApiResult returnBook(Borrow borrow) {
        //检查是否有这条借书记录
        try (PreparedStatement ps2 = connector.getConn().prepareStatement("select * from borrow where card_id=? and book_id=? and borrow_time=? ")) {
            ps2.setInt(1, borrow.getCardId());
            ps2.setInt(2, borrow.getBookId());
            ps2.setLong(3, borrow.getBorrowTime());
            ResultSet rs2 = ps2.executeQuery();
            if (!rs2.next()) {
                System.out.println("The borrow record does not exist");
                return new ApiResult(false, "fail");
            }
        } catch (SQLException e) {
            return new ApiResult(false, "fail");
        }
        //检查是否已经归还
        try (PreparedStatement ps2 = connector.getConn().prepareStatement("select * from borrow where card_id=? and book_id=? and borrow_time=? ")) {
            ps2.setInt(1, borrow.getCardId());
            ps2.setInt(2, borrow.getBookId());
            ps2.setLong(3, borrow.getBorrowTime());
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                if (rs2.getLong("return_time") != 0) {
                    System.out.println("The book has been returned");
                    return new ApiResult(false, "fail");
                }
        }
        } catch (SQLException e) {
            return new ApiResult(false, "fail");
        }
        //更新borrow表
        try (PreparedStatement ps3 = connector.getConn().prepareStatement("update borrow set return_time=? where card_id=? and book_id=? and borrow_time=? ")) {
            borrow.resetReturnTime();
            ps3.setLong(1, borrow.getReturnTime());
            ps3.setInt(2, borrow.getCardId());
            ps3.setInt(3, borrow.getBookId());
            ps3.setLong(4, borrow.getBorrowTime());
            ps3.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error occurred while returning the book");
            return new ApiResult(false, "fail");
        }
        //更新book表
        try (PreparedStatement ps1 = connector.getConn().prepareStatement("select * from book where book_id=?")) {
            ps1.setInt(1, borrow.getBookId());
            ResultSet rs1 = ps1.executeQuery();
            if (!rs1.next()) {
                System.out.println("The book does not exist");
                return new ApiResult(false, "fail");
            }
            //更新库存
            //incBookStock(bookId, int deltaStock);
        }
        catch (Exception e){
            return new ApiResult(false, "fail");
        }
        return incBookStock(borrow.getBookId(), 1);

    }


    @Override
    public ApiResult showBorrowHistory(int cardId) {
        List<BorrowHistories.Item> resultlist = new ArrayList<BorrowHistories.Item>();
        try (PreparedStatement ps = connector.getConn().prepareStatement("select * from borrow where card_id=? ")) {
            //List<BorrowHistories.Item> resultlist = new ArrayList<BorrowHistories.Item>();
            ps.setInt(1, cardId);
            ResultSet rs = ps.executeQuery();
            //rs.next();
            //System.out.println (rs.getInt(1));
            // int count=0;
            while (rs.next()) {
                //   count++;
                // System.out.println (count);
                Book newbook = new Book();
                Borrow newborrow = new Borrow();
                newborrow.setBookId(rs.getInt("book_id"));
                newborrow.setCardId(rs.getInt("card_id"));
                newborrow.setBorrowTime(rs.getLong("borrow_time"));
                newborrow.setReturnTime(rs.getLong("return_time"));
                try (PreparedStatement ps1 = connector.getConn().prepareStatement("select * from book where book_id=?")) {
                    ps1.setInt(1, rs.getInt("book_id"));
                    ResultSet rs2 = ps1.executeQuery();
                    while (rs2.next()) {
                        newbook.setBookId(rs2.getInt("book_id"));
                        newbook.setCategory(rs2.getString("category"));
                        newbook.setTitle(rs2.getString("title"));
                        newbook.setPress(rs2.getString("press"));
                        newbook.setPublishYear(rs2.getInt("publish_year"));
                        newbook.setAuthor(rs2.getString("author"));
                        newbook.setPrice(rs2.getDouble("price"));
                        newbook.setStock(rs2.getInt("stock") - 1);
                        BorrowHistories.Item newitem = new BorrowHistories.Item(cardId, newbook, newborrow);
                        resultlist.add(newitem);
                    }
                }


            }
            Comparator<BorrowHistories.Item> byBorrowTime = Comparator.comparing(BorrowHistories.Item::getBorrowTime).reversed();
            Comparator<BorrowHistories.Item> byId = Comparator.comparing(BorrowHistories.Item::getBookId);
            Comparator<BorrowHistories.Item> byBorrowTimeAndId = byBorrowTime.thenComparing(byId);
            resultlist.sort(byBorrowTimeAndId);
            BorrowHistories result=new BorrowHistories(resultlist);
            commit(connector.getConn());
            return new ApiResult(true, "success",result);
        }catch(SQLException e ){
            return new ApiResult(false, "fail");
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        String insertSQL = "INSERT INTO card VALUES (null, ?, ?, ?)";
        try (PreparedStatement statement = this.connector.getConn().prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            // Set parameters
            statement.setString(1, card.getName());
            statement.setString(2, card.getDepartment());
            statement.setString(3, card.getType().getStr());
            // Execute insertion
            if (isCardDuplicate(card)) {
                return new ApiResult(false, "该借书证已存在，无法重复注册。");
            } else {
                int affectedRows = statement.executeUpdate();
                if (affectedRows > 0) {
                    // Get the generated primary key
                    try (ResultSet rs = statement.getGeneratedKeys()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            card.setCardId(id);
                            commit(connector.getConn());
                            return new ApiResult(true, id); // 获取生成的 card_id
                        } else {
                            return new ApiResult(false, "借书证注册失败，未获取到ID。");
                        }
                    }
                } else {
                    return new ApiResult(false, "借书证注册失败，未影响任何行。");
                }
            }
        } catch (Exception e) {
            return new ApiResult(false, "未实现的功能");
        }
    }

    private boolean isCardDuplicate(Card card) throws SQLException {
        Connection conn = this.connector.getConn();
        String checkSql = "SELECT * FROM card WHERE name = ? AND department = ? AND type = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, card.getName());
            stmt.setString(2, card.getDepartment());
            stmt.setString(3, card.getType().getStr());
            try (ResultSet rs = stmt.executeQuery()) {
                // 如果查询有结果，则表示书籍重复
                return rs.next(); // 只需要检查是否有至少一行数据，因为我们已经通过所有关键列进行了查询
            }
        }
    }
    @Override
    public ApiResult removeCard(int cardId) {
        try(PreparedStatement select_sql = connector.getConn().prepareStatement("select * from borrow where card_id = ?")){
            select_sql.setInt(1,cardId);
            ResultSet rs2= select_sql.executeQuery();
            while(rs2.next()){
                if(rs2.getLong("return_time")==0){
                    System.out.println ("here 1");
                    return new ApiResult(false, "fail");
                }
            }}
        catch(SQLException e ){
            System.out.println ("here 2"+cardId);
            e.printStackTrace();
        }

        try(PreparedStatement select_card_sql = connector.getConn().prepareStatement("select * from card where card_id = ?")) {
            select_card_sql.setInt(1,cardId);
            ResultSet rs=select_card_sql.executeQuery();
            if(!rs.next()){
                System.out.println ("this is"+cardId);
                return new ApiResult(false, "fail");
            }
            try(PreparedStatement delete_card_sql = connector.getConn().prepareStatement("delete from card where card_id=?")){
                delete_card_sql.setInt(1,cardId);
                delete_card_sql.executeUpdate();
                commit(connector.getConn());
                System.out.println ("here 3"+cardId);
                return new ApiResult(true, "success");
            }
        }
        catch(SQLException e ){
            System.out.println ("here 4"+cardId);
            e.printStackTrace();
        }
        System.out.println ("here 5"+cardId);
        return new ApiResult(false, "fail");
    }


    @Override
    public ApiResult showCards() {
        try(PreparedStatement ps = connector.getConn().prepareStatement("select * from card")){
            ResultSet rs=ps.executeQuery();
            List<Card> ResultList = new ArrayList<Card>();

            while(rs.next()){
                Card newcard= new Card();
                newcard.setCardId(rs.getInt("card_id"));
                newcard.setName(rs.getString("name"));
                newcard.setDepartment(rs.getString("department"));
                String s=rs.getString("type");
                if(Objects.equals(s, "S")){
                    s="Student";
                }
                else{
                    s="Teacher";
                }
                Card.CardType c =Card.CardType.valueOf(s);
                newcard.setType(c);
                ResultList.add(newcard);

            }
            ResultList.sort(Comparator.comparingInt(Card::getCardId));
            CardList Results=new CardList(ResultList);
            commit(connector.getConn());
            return new ApiResult(true, "success",Results);
        }
        catch(SQLException e ){
            return new ApiResult(false, "fail");
        }
    }
    @Override
    public ApiResult ModifyCard(int cardId, String name, String department, String type){
        try(PreparedStatement ps = connector.getConn().prepareStatement("update card set name=?,department=?,type=? where card_id=?")){
            ps.setString(1,name);
            ps.setString(2,department);
            ps.setString(3,type);
            ps.setInt(4,cardId);
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
        return new ApiResult(true, "success");
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
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
