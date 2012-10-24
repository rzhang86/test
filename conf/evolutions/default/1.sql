# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table stroke (
  id                        bigint auto_increment not null,
  ip                        varchar(255),
  name                      varchar(255),
  date_time                 datetime,
  x                         integer,
  y                         integer,
  constraint pk_stroke primary key (id))
;




# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table stroke;

SET FOREIGN_KEY_CHECKS=1;

