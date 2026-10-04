import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('5.189.162.46', username='root', password='9nJWv5TMNK9nHgtZI3X', timeout=10)

stdin, stdout, stderr = ssh.exec_command('docker ps --format "{{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}"')
lines = stdout.read().decode('utf-8', errors='ignore').strip().splitlines()
print("=== DOCKER CONTAINERS ===")
for l in lines:
    print(l)

ssh.close()
