create database KUNA;

CREATE TABLE trades (
    id int,
    price Double,
    volume Double,
    funds Double,
    market varchar(255),
    created_at DATETIME,
    side varchar(255),
    UNIQUE KEY (id)
);

CREATE USER 'scala'@'10.2.20.195' IDENTIFIED BY 'scalapass';

grant all on trades to 'scala'@'10.2.20.195';