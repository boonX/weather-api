create table location (
    id uuid primary key,
    name varchar(255)
);

create table users (
    id uuid primary key,
    email varchar(128) not null unique,
    password varchar(255) not null
);

create table subscription (
    id uuid primary key,
    location_id uuid references location,
    user_id uuid references users
);
