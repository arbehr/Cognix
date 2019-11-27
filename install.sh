echo "Executando o script de inicialização da base de dados..."
echo "#Creating user..."
sudo -u postgres psql -c "CREATE USER cognitiva WITH PASSWORD 'rep@cognitiva'"
echo "Tentando deletar a base de dados antiga..."
sudo -u postgres dropdb repositorio
echo "#Criando base local..."
sudo -u postgres createdb -E UTF8 -T template0 --locale=pt_BR.utf8 -O cognitiva repositorio

echo "#Criando as tabelas..."
sudo -u postgres psql repositorio < ./src/main/resources/schema.sql
echo "#Inserindo esquema do postgres..."
sudo -u postgres psql repositorio < ./src/main/resources/postgres_schema.sql
