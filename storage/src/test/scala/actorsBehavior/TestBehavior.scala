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

import actor.{CameraManager, DBCoordinator, DBWriter, Supervisor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.or
import database.MongoDBDriver
import message._
import org.bson.{BsonString, Document}
import org.scalatest.flatspec.AnyFlatSpec
import utils.{ActorTypes, Info, StandardChildProcessCommands}

import scala.collection.immutable.Queue
import scala.util.Random

class TestBehavior extends AnyFlatSpec:

  "The DBWriter" should "connect to the local MongoDB instance via MongoDBDriver and write to it the output of the CameraManager assigned to it" in testDBWriterBehavior()
  "The DBCoordinator" should "connect to the Supervisor via receptionist and assign a DBWriter to each CameraManager with a running child" in testDBCoordinator()

  def testDBWriterBehavior(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val probe = testKit.createTestProbe[Message]()
    val collection = MongoDBDriver().connect()

    Thread.sleep(2000)
    if(collection.isEmpty) fail()

    val dbWriter = testKit.spawn(DBWriter(collection.get, "testCamera"))
    val camera = testKit.spawn(CameraManager(9999).create())
    Thread.sleep(2000)
    val expectedCameraInfo = Info().setSelfRef(camera).setActorType(ActorTypes.CameraManager)

    //reset testCamera entries in DB
    collection.get.deleteMany(Filters.eq("cameraName", "testCamera"))
    Thread.sleep(2000)

    //create test random numbers
    val randomNumber1 = Random().nextInt(10000000)
    val randomNumber2 = Random().nextInt(10000000)
    val expectedList = List(randomNumber1.toString, randomNumber2.toString).sorted

    //start a new CM child (echo pwshell program) with the first random as line parameter
    val powershellCommand: String = if (System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"
    val configCommand = Queue(powershellCommand + " -ExecutionPolicy Bypass -File ../application/src/test/powershell/testCameraManagerScript.ps1 ", randomNumber1.toString)
    camera ! Config(probe.ref, configCommand)
    probe.expectMessage(java.time.Duration.ofSeconds(10), ConfigServiceSuccess(expectedCameraInfo))
    dbWriter ! SwitchToCamera(camera)
    Thread.sleep(5000)

    //send second number as keyboard input
    camera ! Input(probe.ref, randomNumber2.toString)
    probe.expectMessage(InputServiceSuccess(expectedCameraInfo))

    //wait for the DBWriter to write both of them in the DB (as specified in its Sink function)
    Thread.sleep(2000)

    //get the numbers from the DB to assert that they were saved correctly
    val resIterator = collection.get.find(Filters.eq("cameraName", BsonString("testCamera"))).iterator()
    assert(resIterator.available() == 2)
    val resList = List(resIterator.next(), resIterator.next())
    assert(resList
      .sortBy(doc => doc.get("value").toString)
      .map(doc => doc.get("value").toString)
      .equals(expectedList))

    //test passed: cleanup
    collection.get.deleteMany(Filters.eq("cameraName", "testCamera"))
    camera ! Input(probe.ref, StandardChildProcessCommands.Kill.command)
    probe.expectMessage(InputServiceSuccess(expectedCameraInfo))

  def testDBCoordinator(): Unit =
    val testKit: ActorTestKit = ActorTestKit()
    val collection = MongoDBDriver().connect().getOrElse(fail())
    val probe = testKit.createTestProbe[Message]()
    val supervisor = testKit.spawn(Supervisor().create())
    val camera1 = testKit.spawn(CameraManager(9999).create())
    val camera2 = testKit.spawn(CameraManager(9999).create())
    val expectedCameraInfo1 = Info().setSelfRef(camera1).setActorType(ActorTypes.CameraManager)
    val expectedCameraInfo2 = Info().setSelfRef(camera2).setActorType(ActorTypes.CameraManager)
    val dbCoordinator = testKit.spawn(DBCoordinator())

    def getTestEntries =
      collection
      .find(or(Filters.eq("cameraName", BsonString(camera1.toString)), Filters.eq("cameraName", BsonString(camera2.toString))))
      .iterator()

    //check if there are DB entries with the same camera refs
    assert(getTestEntries.available() == 0)

    //create test random numbers
    val randoms = for i <- Range(0,4) yield Random().nextInt(10000000)

    val powershellCommand: String = if (System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"

    def runTwoInputsOnACamera(camera: ActorRef[Message], inputs: List[String], expectedInfo: Info) =
      val configCommand = Queue(powershellCommand + " -ExecutionPolicy Bypass -File ../application/src/test/powershell/testCameraManagerScript.ps1 ", inputs.head)
      //config the camera and let the DBCoordinator create the DBWriter that will save its output (first input as command parameter)
      camera ! Config(probe.ref, configCommand)
      Thread.sleep(2000)
      probe.expectMessage(ConfigServiceSuccess(expectedInfo))
      Thread.sleep(10000)

      //send second input as keyboard input
      camera ! Input(probe.ref, inputs(1))
      probe.expectMessage(InputServiceSuccess(expectedInfo))
      Thread.sleep(5000)

      //kill the child process
      camera ! Input(probe.ref, StandardChildProcessCommands.Kill.command)
      probe.expectMessage(InputServiceSuccess(expectedInfo))

    runTwoInputsOnACamera(camera1, randoms.sorted.take(2).map(_.toString).toList, expectedCameraInfo1)
    runTwoInputsOnACamera(camera2, randoms.sorted.drop(2).map(_.toString).toList, expectedCameraInfo2)

    //check if the randoms were correctly saved in the database in the right format
    val result = getTestEntries
    assert(result.available() == 4)

    val expectedDocuments =
      randoms.sorted
      .map(rand => (rand -> randoms.sorted.indexOf(rand)))
      .map((rand, index) =>
        if (index < 2)
          Document("cameraName", camera1.toString).append("value", rand.toString)
        else
          Document("cameraName", camera2.toString).append("value", rand.toString))
      .toList

    result
      .forEachRemaining(doc =>
        assert(expectedDocuments.contains(Document("cameraName",  doc.get("cameraName")).append("value", doc.get("value")))))

    //test passed: cleanup
    collection.deleteMany(or(Filters.eq("cameraName", camera1.toString), Filters.eq("cameraName", camera2.toString)))
