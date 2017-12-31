CREATE TABLE PUBLIC.domainblacklist (
name VARCHAR(255) NOT NULL,
timestamp TIMESTAMP DEFAULT 'now',
PRIMARY KEY ( name )
);
CREATE TABLE PUBLIC.serverblacklist (
name VARCHAR(255) NOT NULL,
timestamp TIMESTAMP DEFAULT 'now',
PRIMARY KEY ( name )
);
CREATE TABLE PUBLIC.serverwhitelist (
name VARCHAR(255) NOT NULL,
timestamp TIMESTAMP DEFAULT 'now',
PRIMARY KEY ( name ));
