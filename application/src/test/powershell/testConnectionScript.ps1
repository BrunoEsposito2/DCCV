$IP = '127.0.0.1'
$PORT = 9999
Start-Sleep -Seconds 2
# Create a TCP client and connect to the server
$client = New-Object System.Net.Sockets.TcpClient($IP, $PORT)
Start-Sleep -Seconds 30