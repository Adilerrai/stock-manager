import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('5.189.162.46', username='root', password='9nJWv5TMNK9nHgtZI3X')

cmd = 'docker exec pgsql.prod psql -U root -d pointvente_db -c "SELECT point_de_vente_id, nom_entreprise, nom_banque, compte_bancaire_rib FROM entreprise_profiles;"'
stdin, stdout, stderr = ssh.exec_command(cmd)
print("STDOUT:", stdout.read().decode())
print("STDERR:", stderr.read().decode())

ssh.close()
