CREATE TABLE TestCase_category ( 
	category_name varchar(50),
	ID int NOT NULL,
	ID_testcase int
)
;

ALTER TABLE TestCase_category ADD CONSTRAINT PK_TestCase_category 
	PRIMARY KEY CLUSTERED (ID)
;




