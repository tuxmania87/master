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


