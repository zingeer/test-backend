alter table budget
    add column author integer
        references author(id) on delete cascade on update cascade;