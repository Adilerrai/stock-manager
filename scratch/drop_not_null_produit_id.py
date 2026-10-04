import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('5.189.162.46', username='root', password='9nJWv5TMNK9nHgtZI3X', timeout=10)

sql = "ALTER TABLE lignes_facture ALTER COLUMN produit_id DROP NOT NULL;"

stdin, stdout, stderr = client.exec_command(f'docker exec -i pgsql.prod psql -U postgres -d pointvente_db -c "{sql}"')
out = stdout.read().decode('utf-8', errors='ignore')
err = stderr.read().decode('utf-8', errors='ignore')

print("OUT:", out)
if err:
    print("ERR:", err)

client.close()
