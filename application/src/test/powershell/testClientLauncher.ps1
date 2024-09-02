param($s)
$IP = '127.0.0.1'
$PORT = 9999

# Create a TCP client and connect to the server
$client = New-Object System.Net.Sockets.TcpClient($IP, $PORT)
$stream = $client.GetStream()
$writer = New-Object System.IO.StreamWriter($stream)

$writer.WriteLine($s)
echo $s
$writer.Flush()
Start-Sleep -Seconds 5
$userKey = Read-Host "Please enter a key"
echo $userKey
$writer.WriteLine($userKey)

# Close the connection 
$writer.Close()
$client.Close()
