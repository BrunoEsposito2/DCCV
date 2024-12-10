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

package actor

import akka.util.ByteString
import com.mongodb.client.MongoCollection
import message.Message
import org.bson.Document
import utils.Info

object DBWriter:
  def apply(mongoCollection: MongoCollection[Document], cameraName: String): Behavior =
    new DBWriter(mongoCollection, cameraName).create()

private class DBWriter(mongoCollection: MongoCollection[Document], cameraName: String) extends AbstractClient:

  override def onMessage(msg: Message, clientInfo: Info): Unit =
    msg match
      case _ =>

  override def startingSinkFunction(): ByteString => Unit =
    bs =>
      val doc = Document("cameraName", cameraName).append("value", bs.utf8String.strip())
      mongoCollection.insertOne(doc)