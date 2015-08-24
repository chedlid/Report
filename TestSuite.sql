CREATE TABLE TestSuite ( 
	asserts varchar(50),
	 description varchar(max),
	executed varchar(50),
	name varchar(50),
	reason_message varchar(max),
	result varchar(50),
	success varchar(50),
	time varchar(50),
	type varchar(50),
	ID int NOT NULL,
	failure_message varchar(max),
	failure_stacktrace varchar(max),
	ID_parent_test int
)
;

ALTER TABLE TestSuite ADD CONSTRAINT PK_TestSuite 
	PRIMARY KEY CLUSTERED (ID)
;














