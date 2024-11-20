param($s)
$IP = '127.0.0.1'
$PORT = 9999

# Create a TCP client and connect to the server
$client = New-Object System.Net.Sockets.TcpClient($IP, $PORT)
$stream = $client.GetStream()
$writer = New-Object System.IO.StreamWriter($stream)
Start-Sleep -Seconds 6
$writer.WriteLine($s)
echo $s
$writer.Flush()

# Loop to get user input
while ($true)
{
    $userKey = Read-Host "Please enter a key"
    if ($userKey -eq "k")
    {
        break
    }
    echo $userKey
    $writer.WriteLine($userKey)
    $writer.Flush()
}

# Close the connection (this part will not be reached due to the infinite loop, but is here for completeness)
$writer.Close()
$client.Close()
