import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('5.189.162.46', username='root', password='9nJWv5TMNK9nHgtZI3X', timeout=10)

stdin, stdout, stderr = ssh.exec_command('docker logs --tail 250 pointvente-app-api')
out = stdout.read().decode('utf-8', errors='ignore')
err = stderr.read().decode('utf-8', errors='ignore')

print("=== LOGS pointvente-app-api (STDOUT) ===")
print(out)
if err:
    print("=== LOGS pointvente-app-api (STDERR) ===")
    print(err)

ssh.close()
