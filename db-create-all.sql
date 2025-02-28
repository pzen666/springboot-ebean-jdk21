-- apply changes
create table test (
  video_id                      bigint generated by default as identity not null,
  video_name                    varchar(255),
  name                          varchar(255),
  constraint pk_test primary key (video_id)
);

create table test2 (
  video_id                      bigint generated by default as identity not null,
  created_at                    timestamptz not null,
  created_at2                   timestamptz not null,
  video_name                    varchar(255),
  name                          varchar(255),
  constraint pk_test2 primary key (video_id)
);

create table video (
  video_id                      varchar(255) not null,
  constraint pk_video primary key (video_id)
);

