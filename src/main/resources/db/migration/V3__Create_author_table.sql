create table author
(
    id   serial primary key,
    fio  text   not null,
    created_at   timestamp default now()
);