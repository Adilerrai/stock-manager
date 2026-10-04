import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('5.189.162.46', username='root', password='9nJWv5TMNK9nHgtZI3X', timeout=10)

stdin, stdout, stderr = ssh.exec_command('docker ps')
ps_output = stdout.read().decode('utf-8', errors='ignore')
print("=== DOCKER PS ===")
print(ps_output)

# Find containers with 'stock' in name or all containers
stdin, stdout, stderr = ssh.exec_command('docker ps --filter "name=stock" --format "{{.Names}}"')
stock_containers = stdout.read().decode('utf-8', errors='ignore').strip().splitlines()

if not stock_containers:
    stdin, stdout, stderr = ssh.exec_command('docker ps --format "{{.Names}}"')
    all_containers = stdout.read().decode('utf-8', errors='ignore').strip().splitlines()
    print("No container matching 'name=stock'. All running containers:", all_containers)
    target_containers = all_containers
else:
    target_containers = stock_containers

for container in target_containers:
    print(f"\n=== LOGS FOR {container} (tail 100) ===")
    stdin, stdout, stderr = ssh.exec_command(f'docker logs --tail 100 {container}')
    logs = stdout.read().decode('utf-8', errors='ignore')
    err_logs = stderr.read().decode('utf-8', errors='ignore')
    if logs:
        print(logs)
    if err_logs:
        print(err_logs)

ssh.close()
