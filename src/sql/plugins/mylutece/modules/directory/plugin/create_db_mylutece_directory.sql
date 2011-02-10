DROP TABLE IF EXISTS mylutece_directory_directory;
DROP TABLE IF EXISTS mylutece_directory_user;
DROP TABLE IF EXISTS mylutece_directory_user_role;
DROP TABLE IF EXISTS mylutece_directory_user_group;
DROP TABLE IF EXISTS mylutece_directory_mapping;
DROP TABLE IF EXISTS mylutece_directory_parameter;

--
-- Table struture for mylutece_directory_directory
--
CREATE TABLE mylutece_directory_directory (
	id_directory int NOT NULL,
	PRIMARY KEY (id_directory)
);

--
-- Table struture for mylutece_directory_user
--
CREATE TABLE mylutece_directory_user (
	id_record int NOT NULL,
	user_login varchar(100) default '' NOT NULL,
	user_password varchar(100) default '' NOT NULL,
	activated SMALLINT DEFAULT '0',
	PRIMARY KEY (id_record)
);

--
-- Table struture for mylutece_directory_user_role
--
CREATE TABLE mylutece_directory_user_role (
	id_record int NOT NULL,
	role_key varchar(50) default '' NOT NULL,
	PRIMARY KEY (id_record,role_key)
);

--
-- Table struture for mylutece_directory_user_group
--
CREATE TABLE mylutece_directory_user_group (
	id_record int NOT NULL,
	group_key varchar(100) default '' NOT NULL,
	PRIMARY KEY (id_record,group_key)
);

--
-- Table struture for mylutece_directory_mapping
--
CREATE TABLE mylutece_directory_mapping (
	id_entry int NOT NULL,
	attribute_key varchar(100) default '' NOT NULL,
	PRIMARY KEY (id_entry)
);

--
-- Table structure for table mylutece_directory_parameter
--
CREATE TABLE mylutece_directory_parameter (
	parameter_key varchar(100) NOT NULL,
	parameter_value varchar(100) NOT NULL,
	PRIMARY KEY (parameter_key)
);
