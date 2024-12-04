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

package actorsBehavior

import actor.{AbstractService, CameraManager, ConfigureClientSink, DBWriter, Supervisor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import com.mongodb.client.model.Filters
import database.MongoDBDriver
import message.{CameraMap, *}
import org.scalatest.flatspec.AnyFlatSpec
import utils.ChildStatuses.{Idle, Running}
import utils.{ActorTypes, Info, StandardChildProcessCommands}

import java.util.concurrent.TimeUnit
import scala.collection.immutable.Queue
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

class TestBehavior extends AnyFlatSpec:

  "The DBWriter" should "connect to the local MongoDB instance via MongoDBDriver and write to it a CameraManager output" in testDBWriterBehavior()

  def testDBWriterBehavior(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val probe = testKit.createTestProbe[Message]()
    val collection = MongoDBDriver().connect()
    Thread.sleep(2000)
    if(collection.isEmpty) fail()
    val dbWriter = testKit.spawn(DBWriter(collection.get, "testCamera"))

    val camera = testKit.spawn(CameraManager(9999).create())
    val expectedCameraInfo = Info().setSelfRef(camera).setActorType(ActorTypes.CameraManager)

    //reset testCamera entries in DB
    collection.get.deleteMany(Filters.eq("cameraName", "testCamera"))

    val randomNumber1 = Random().nextInt()
    val randomNumber2 = Random().nextInt()
    val powershellCommand: String = if (System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"
    val configCommand = Queue(powershellCommand + " -ExecutionPolicy Bypass -File ../application/src/test/powershell/testCameraManagerScript.ps1 ", randomNumber1.toString)

    camera ! Config(probe.ref, configCommand)
    probe.expectMessage(ConfigServiceSuccess(expectedCameraInfo))
    dbWriter ! SwitchToCamera(camera)
    Thread.sleep(2000)
    camera ! Input(probe.ref, randomNumber2.toString)

    val res = collection.get.find(Filters.eq("cameraName", "testCamera"))
    println(res)


