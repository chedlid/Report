CREATE TABLE TestSuite_property ( 
	name varchar(50),
	value varchar(50),
	ID int NOT NULL,
	ID_testsuite int
)
;

ALTER TABLE TestSuite_property ADD CONSTRAINT PK_TestSuite_property 
	PRIMARY KEY CLUSTERED (ID)
;





