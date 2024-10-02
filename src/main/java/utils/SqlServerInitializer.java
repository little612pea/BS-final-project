package utils;

public class SqlServerInitializer implements DBInitializer {

    @Override
    public String sqlDropProduct() {
        return "IF OBJECT_ID('dbo.product', 'U') IS NOT NULL DROP TABLE dbo.product;";
    }

    @Override
    public String sqlDropCard() {
        return "IF OBJECT_ID('dbo.card', 'U') IS NOT NULL DROP TABLE dbo.card;";
    }

    @Override
    public String sqlDropBorrow() {
        return "IF OBJECT_ID('dbo.borrow', 'U') IS NOT NULL DROP TABLE dbo.borrow;";
    }

    @Override
    public String sqlCreateProduct() {
        return "create table product (\n" +
                "    productId int not null identity,\n" +
                "    comment varchar(63) not null,\n" +
                "    title varchar(63) not null,\n" +
                "    shop varchar(63) not null,\n" +
                "    deal int not null,\n" +
                "    img_url varchar(63) not null,\n" +
                "    price decimal(7, 2) not null default 0.00,\n" +
                "    source int not null default 0,\n" +
                "    primary key (productId),\n" +
                "    unique (comment, shop, img_url, title, deal)\n" +
                ");";
    }

    @Override
    public String sqlCreateCard() {
        return "create table card (\n" +
                "    card_id int not null identity,\n" +
                "    name varchar(63) not null,\n" +
                "    department varchar(63) not null,\n" +
                "    type char(1) not null,\n" +
                "    primary key (card_id),\n" +
                "    unique (department, type, name),\n" +
                "    check ( type in ('T', 'S') )\n" +
                ");";
    }

    @Override
    public String sqlCreateBorrow() {
        return "create table borrow (\n" +
                "    card_id int not null,\n" +
                "    productId int not null,\n" +
                "    borrow_time bigint not null,\n" +
                "    return_time bigint not null default 0,\n" +
                "    primary key (card_id, productId, borrow_time),\n" +
                "    foreign key (card_id) references card(card_id) on delete cascade on update cascade,\n" +
                "    foreign key (productId) references product(productId) on delete cascade on update cascade\n" +
                ");";
    }
}
