CREATE TABLE users (
  id INTEGER PRIMARY KEY NOT NULL,
  name varchar(150) NOT NULL,
  password varchar(150) NOT NULL,
  isDozent tinyint(1) DEFAULT 0
); 

CREATE TABLE external_database (
  id INTEGER PRIMARY KEY NOT NULL,
  uri varchar(100) NOT NULL,
  dbname varchar(100) NOT NULL,
  username varchar(100) NOT NULL,
  password varchar(100) NOT NULL,
  typ varchar(100) NOT NULL
);



CREATE TABLE dbschema (
  id INTEGER PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  schem TEXT NOT NULL
); 


CREATE TABLE attempts (
  id INTEGER NOT NULL PRIMARY KEY,
  userid INTEGER REFERENCES user(id),
  taskid INTEGER REFERENCES tasks(id),
  timeat DATETIME NOT NULL,
  sqlstatement varchar(200) NOT NULL,
  correct tinyint(2) NOT NULL
); 



CREATE TABLE samplesolutions (
  id INTEGER PRIMARY KEY NOT NULL,
  taskid INTEGER REFERENCES tasks(id),
  sqlstatement varchar(500) NOT NULL
); 

CREATE TABLE tasks (
  id INTEGER PRIMARY KEY NOT NULL,
  schemaid INTEGER REFERENCES dbschema(id),
  respectColumnorder tinyint(1) NOT NULL,
  description TEXT NOT NULL,
  createdAt DATETIME NOT NULL
); 

CREATE TABLE tasks_db (
  taskid INTEGER REFERENCES tasks(id),
  dbid INTEGER REFERENCES external_database(id),
  PRIMARY KEY (taskid, dbid)
); 

INSERT INTO dbschema (id, name, schem) VALUES(1, 'emp, dept database', 'create table emp (empno int not null, ename varchar(500), job varchar(500), mgr int, hiredate datetime, sal numeric(4,2), comm int, deptno int);\r\n\r\ncreate table dept (deptno int, dname varchar(500), location varchar(500));');
INSERT INTO samplesolutions (id, taskid, sqlstatement) VALUES(1, 1, 'select ename from emp e where e.sal > (select avg(x.sal) from emp x);');
INSERT INTO tasks (schemaid, respectColumnorder, id, description, createdAt) VALUES(1, 0, 1, 'Geben Sie die Namen aller Angestellten aus, die mehr als das Durchschnittsgehalt erhalten.', '2013-05-28 10:48:00');
INSERT INTO users (id, name, password, isDozent) VALUES(1, 'admin', 'secure1234', 1);



