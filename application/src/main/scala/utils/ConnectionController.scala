package utils

import java.io.InputStream
import java.net.{ServerSocket, Socket, SocketTimeoutException}

object ConnectionController:
  def apply(port: Integer): ConnectionController = new ConnectionController(port)
  private def apply(port:Integer, client:Socket, server:ServerSocket) = new ConnectionController(port, Option(client), Option(server))

class ConnectionController(val port:Integer, client:Option[Socket] = Option.empty, server:Option[ServerSocket] = Option.empty):
  def enstablishConnection(): ConnectionController =
    if(client.nonEmpty || server.nonEmpty) closeConnection().enstablishConnection()
    else
      val newServer = ServerSocket(port)
      newServer.setSoTimeout(3000)
      try
        val newClient = newServer.accept()
        ConnectionController(port, newClient, newServer)
      catch
        case e: SocketTimeoutException =>
          newServer.close()
          throw e

  def closeConnection(): ConnectionController =
    server match
      case Some(openedServer) =>
        client match
          case Some(connectedClient) =>
            connectedClient.close()
          case None =>
        openedServer.close()
      case None =>
    ConnectionController(port)

  def getClientInput: Option[InputStream] =
    client match
      case Some(connectedClient) => Option(connectedClient.getInputStream)
      case None => Option.empty



