drop table if exists `borrow`;
drop table if exists `card`;
drop table if exists `book`;
drop table if exists `users`;
drop table if exists `goods_tb`;
drop table if exists `goods_jd`;


create table `users` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
)engine=innodb charset=utf8mb4;

create table `goods_tb` (
     title VARCHAR(255) UNIQUE NOT NULL,
     price VARCHAR(255) UNIQUE NOT NULL,
     deal VARCHAR(255) NOT NULL,
     location VARCHAR(255) NOT NULL,
     shop VARCHAR(255) NOT NULL,
     isPostFree BOOLEAN
)engine=innodb charset=utf8mb4;

create table `goods_jd` (
    title VARCHAR(255) UNIQUE NOT NULL,
    price VARCHAR(255) UNIQUE NOT NULL,
    comment VARCHAR(255) NOT NULL,
    shop VARCHAR(255) NOT NULL
)engine=innodb charset=utf8mb4;

create table `book` (
    `book_id` int not null auto_increment,
    `category` varchar(63) not null,
    `title` varchar(63) not null,
    `press` varchar(63) not null,
    `publish_year` int not null,
    `author` varchar(63) not null,
    `price` decimal(7, 2) not null default 0.00,
    `stock` int not null default 0,
    primary key (`book_id`),
    unique (`category`, `press`, `author`, `title`, `publish_year`)
) engine=innodb charset=utf8mb4;

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
  `book_id` int not null,
  `borrow_time` bigint not null,
  `return_time` bigint not null default 0,
  primary key (`card_id`, `book_id`, `borrow_time`),
  foreign key (`card_id`) references `card`(`card_id`) on delete cascade on update cascade,
  foreign key (`book_id`) references `book`(`book_id`) on delete cascade on update cascade
) engine=innodb charset=utf8mb4;