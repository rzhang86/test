# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table stroke (
  id                        bigint not null,
  ip                        varchar(255),
  date                      timestamp,
  x                         integer,
  y                         integer,
  constraint pk_stroke primary key (id))
;

create sequence stroke_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists stroke;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists stroke_seq;

