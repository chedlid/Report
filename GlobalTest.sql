CREATE TABLE GlobalTest ( 
	date varchar(50),
	errors int,
	failures int,
	ignored int,
	inconclusive int,
	invalid int,
	name varchar(50),
	notRun int,
	skipped int,
	time varchar(50),
	total int,
	currentCulture varchar(50),
	currentUICulture varchar(50),
	ID int NOT NULL,
	ID_child_test int
)
;

ALTER TABLE GlobalTest ADD CONSTRAINT PK_GlobalTest 
	PRIMARY KEY CLUSTERED (ID)
;
















