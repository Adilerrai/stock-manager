import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('5.189.162.46', username='root', password='9nJWv5TMNK9nHgtZI3X', timeout=10)

sql = """
UPDATE entreprise_profiles SET
    nom_banque = NULL,
    compte_bancaire_rib = NULL
WHERE point_de_vente_id = 9;
"""

stdin, stdout, stderr = client.exec_command(f'docker exec -i pgsql.prod psql -U postgres -d pointvente_db << "EOF"\n{sql}\nEOF')
out = stdout.read().decode('utf-8', errors='ignore')
print("OUT:", out)

stdin, stdout, stderr = client.exec_command("docker exec -i pgsql.prod psql -U postgres -d pointvente_db -c 'SELECT point_de_vente_id, nom_entreprise, nom_banque, compte_bancaire_rib FROM entreprise_profiles;'")
print(stdout.read().decode('utf-8', errors='ignore'))

client.close()
