drop table if exists `borrow`;
drop table if exists `card`;
drop table if exists `product`;
drop table if exists `users`;


create table `users` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
)engine=innodb charset=utf8mb4;

create table product
(
    productId  int auto_increment primary key,
    comment    varchar(255) null,
    title      varchar(255) not null,
    shop       varchar(255) not null,
    deal       varchar(255) null,
    img_url    varchar(1000) null,
    price      double not null,
    source     varchar(1000) not null
)engine=innodb charset=utf8mb4;

create table `card` (
    `card_id` int not null auto_increment,
    `name` varchar(63) not null,
    `department` varchar(63) not null,
    `type` char(1) not null,
    primary key (`card_id`),
    unique (`department`, `type`, `name`),
    check ( `type` in ('T', 'S') )
) engine=innodb charset=utf8mb4;

create table `borrow` (
  `card_id` int not null,
  `productId` int not null,
  `borrow_time` bigint not null,
  `return_time` bigint not null default 0,
  primary key (`card_id`, `productId`, `borrow_time`),
  foreign key (`card_id`) references `card`(`card_id`) on delete cascade on update cascade,
  foreign key (`productId`) references `product`(`productId`) on delete cascade on update cascade
) engine=innodb charset=utf8mb4;