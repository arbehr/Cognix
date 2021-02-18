echo "Executando o script de inicialização da base de dados..."
echo "#Creating user..."
sudo -u postgres psql -c "CREATE USER re-mar WITH PASSWORD 'teste123'"
echo "Tentando deletar a base de dados antiga..."
sudo -u postgres dropdb repositorio
echo "#Criando base local..."
sudo -u postgres createdb -E UTF8 -T template0 --locale=pt_PT.utf8 -O re-mar repositorio

echo "#Criando as tabelas..."
sudo -u postgres psql repositorio < ./src/main/resources/schema.sql
echo "#Inserindo esquema do postgres..."
sudo -u postgres psql repositorio < ./src/main/resources/postgres_schema.sql
