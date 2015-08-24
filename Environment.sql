CREATE TABLE Environment ( 
	clrVersion varchar(50),
	cwd varchar(50),
	machine_name varchar(50),
	nunitVersion varchar(50),
	osVersion varchar(50),
	platform varchar(50),
	user varchar(50),
	userDomain varchar(50),
	ID int NOT NULL,
	ID_global_test int
)
;

ALTER TABLE Environment ADD CONSTRAINT PK_Environment 
	PRIMARY KEY CLUSTERED (ID)
;











