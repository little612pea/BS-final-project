IF OBJECT_ID('dbo.borrow', 'U') IS NOT NULL DROP TABLE dbo.borrow;
IF OBJECT_ID('dbo.product', 'U') IS NOT NULL DROP TABLE dbo.product;
IF OBJECT_ID('dbo.card', 'U') IS NOT NULL DROP TABLE dbo.card;

create table product (
    productId int not null identity,
    comment varchar(63) not null,
    title varchar(63) not null,
    shop varchar(63) not null,
    deal int not null,
    img_url varchar(63) not null,
    price decimal(7, 2) not null default 0.00,
    source int not null default 0,
    primary key (productId),
    unique (comment, shop, img_url, title, deal)
);

create table card (
    card_id int not null identity,
    name varchar(63) not null,
    department varchar(63) not null,
    type char(1) not null,
    primary key (card_id),
    unique (department, type, name),
    check ( type in ('T', 'S') )
);

create table borrow (
    card_id int not null,
    productId int not null,
    borrow_time bigint not null,
    return_time bigint not null default 0,
    primary key (card_id, productId, borrow_time),
    foreign key (card_id) references card(card_id) on delete cascade on update cascade,
    foreign key (productId) references product(productId) on delete cascade on update cascade
);