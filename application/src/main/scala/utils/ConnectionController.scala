/*
 * Distributed Cluster for Computer Vision
 * Copyright (C) 2024 Andrea Ingargiola, Bruno Esposito
 * andrea.ingargiola@studio.unibo.it
 * bruno.esposito@studio.unibo.it
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

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



