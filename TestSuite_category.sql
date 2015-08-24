CREATE TABLE TestSuite_category ( 
	category_name varchar(50),
	ID int NOT NULL,
	ID_testsuite int
)
;

ALTER TABLE TestSuite_category ADD CONSTRAINT PK_Categories 
	PRIMARY KEY CLUSTERED (ID)
;




