CREATE TABLE TestCase_property ( 
	name varchar(50),
	value varchar(50),
	ID int NOT NULL,
	ID_testcase int
)
;

ALTER TABLE TestCase_property ADD CONSTRAINT PK_Property 
	PRIMARY KEY CLUSTERED (ID)
;





