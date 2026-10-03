-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
CREATE TABLE shift_post (
 id bigint AUTO_INCREMENT PRIMARY KEY,version bigint NOT NULL,code varchar(60) NOT NULL UNIQUE,name varchar(120) NOT NULL,department_id bigint NOT NULL,enabled boolean NOT NULL,FOREIGN KEY(department_id) REFERENCES department(id));
CREATE TABLE shift_qualification (
 id bigint AUTO_INCREMENT PRIMARY KEY,version bigint NOT NULL,account_id bigint NOT NULL,post_id bigint NOT NULL,valid_from date NOT NULL,valid_until date NOT NULL,enabled boolean NOT NULL,UNIQUE(account_id,post_id),FOREIGN KEY(account_id) REFERENCES account(id),FOREIGN KEY(post_id) REFERENCES shift_post(id),CHECK(valid_until>=valid_from));
CREATE TABLE scheduled_shift (
 id bigint AUTO_INCREMENT PRIMARY KEY,version bigint NOT NULL,post_id bigint NOT NULL,department_id bigint NOT NULL,employee_id bigint NOT NULL,title varchar(160) NOT NULL,category varchar(60) NOT NULL,note varchar(2000) NOT NULL,status varchar(20) NOT NULL,starts_at timestamp(6) NOT NULL,ends_at timestamp(6) NOT NULL,published_at timestamp(6),acknowledged_at timestamp(6),assignment_revision int NOT NULL,rest_gap_hours int NOT NULL,created_at timestamp(6) NOT NULL,updated_at timestamp(6) NOT NULL,FOREIGN KEY(post_id) REFERENCES shift_post(id),FOREIGN KEY(department_id) REFERENCES department(id),FOREIGN KEY(employee_id) REFERENCES account(id),CHECK(ends_at>starts_at),CHECK(rest_gap_hours>=0 AND rest_gap_hours<=24));
CREATE TABLE shift_coverage (
 id bigint AUTO_INCREMENT PRIMARY KEY,version bigint NOT NULL,shift_id bigint NOT NULL,requester_id bigint NOT NULL,target_id bigint NOT NULL,assignment_revision int NOT NULL,status varchar(20) NOT NULL,note varchar(1000) NOT NULL,created_at timestamp(6) NOT NULL,updated_at timestamp(6) NOT NULL,FOREIGN KEY(shift_id) REFERENCES scheduled_shift(id),FOREIGN KEY(requester_id) REFERENCES account(id),FOREIGN KEY(target_id) REFERENCES account(id),CHECK(requester_id<>target_id));
CREATE TABLE shift_unavailability (
 id bigint AUTO_INCREMENT PRIMARY KEY,version bigint NOT NULL,account_id bigint NOT NULL,department_id bigint NOT NULL,starts_at timestamp(6) NOT NULL,ends_at timestamp(6) NOT NULL,note varchar(500) NOT NULL,status varchar(20) NOT NULL,created_at timestamp(6) NOT NULL,FOREIGN KEY(account_id) REFERENCES account(id),FOREIGN KEY(department_id) REFERENCES department(id),CHECK(ends_at>starts_at));
CREATE TABLE roster_event (
 id bigint AUTO_INCREMENT PRIMARY KEY,shift_id bigint NOT NULL,coverage_id bigint,actor varchar(60) NOT NULL,action varchar(40) NOT NULL,note varchar(1000) NOT NULL,snapshot text NOT NULL,created_at timestamp(6) NOT NULL,FOREIGN KEY(shift_id) REFERENCES scheduled_shift(id),FOREIGN KEY(coverage_id) REFERENCES shift_coverage(id));
CREATE TABLE roster_command (
 id bigint AUTO_INCREMENT PRIMARY KEY,actor varchar(60) NOT NULL,request_key varchar(80) NOT NULL,fingerprint varchar(64) NOT NULL,kind varchar(20) NOT NULL,object_id bigint NOT NULL,UNIQUE(actor,request_key));
CREATE INDEX ix_shift_employee ON scheduled_shift(employee_id,status,starts_at,ends_at);
CREATE INDEX ix_shift_department ON scheduled_shift(department_id,status,starts_at);
CREATE INDEX ix_coverage_shift ON shift_coverage(shift_id,status);
CREATE INDEX ix_coverage_target ON shift_coverage(target_id,status);
CREATE INDEX ix_unavailability_account ON shift_unavailability(account_id,status,starts_at);
CREATE INDEX ix_roster_event ON roster_event(shift_id,id);
