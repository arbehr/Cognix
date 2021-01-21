ALTER TABLE users ADD UNIQUE (login);
ALTER TABLE documents ADD UNIQUE (obaa_entry);

-- Insert users
INSERT INTO users (login, password, name, permissions, role)
    VALUES ('admin', md5('teste'), 'Administrador Geral', 'PERM_CREATE_DOC,PERM_MANAGE_DOC,PERM_VIEW,PERM_MANAGE_USERS', 'root');
INSERT INTO users (login, password, name, permissions, role)
    VALUES ('anonymous', md5('anonymous'), 'Usuário anônimo', '', 'reader');

ALTER TABLE documents OWNER TO edumar;
ALTER TABLE files OWNER TO edumar;
ALTER TABLE users OWNER TO edumar;
ALTER TABLE subject OWNER TO edumar;
