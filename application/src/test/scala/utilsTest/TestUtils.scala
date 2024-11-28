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

package utilsTest

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import message.{Message, Pong}
import utils.ActorTypes.{Undefined, Utility}
import utils.{ConnectionController, Info}

import scala.sys.process.*
import java.net.{ServerSocket, SocketTimeoutException}

class TestUtils extends AnyFlatSpec:
  "An Info" should "update itself recursevly mantaining not-overriden informations" in testInfo()
  "A ConnectionController" should "open and manage TCP sockets in a functional way" in testConnectionController()

  val testKit: ActorTestKit = ActorTestKit()
  val powershellCommand: String = if(System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"

  def testInfo(): Unit =
    val probe = testKit.createTestProbe[Message]()
    val info = Info()
    probe ! Pong(info)
    probe.expectMessage(Pong(Info()))
    probe ! Pong(info.addRef(probe.ref))
    probe.expectMessage(Pong(Info(null, Set(probe.ref), Undefined)))
    probe ! Pong(info.addRef(probe.ref).setSelfRef(probe.ref).resetLinkedActors())
    probe.expectMessage(Pong(Info(probe.ref, Set(), Undefined)))
    probe ! Pong(info.setActorType(Utility))
    probe.expectMessage(Pong(Info(null, Set(), Utility)))

  def testConnectionController(): Unit =
    val cc = ConnectionController(9999)
    assertThrows[SocketTimeoutException](cc.enstablishConnection())
    val testOccupiedAddress = ServerSocket(9999)
    assert(testOccupiedAddress.getLocalPort == 9999)
    testOccupiedAddress.close()
    Process(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testConnectionScript.ps1").run()
    try
      val enstablishedCC = cc.enstablishConnection()
      Thread.sleep(5000)
      enstablishedCC.closeConnection()
    catch
      case e:Exception => fail()
    val testFreeAddress = ServerSocket(9999)
    assert(testOccupiedAddress.getLocalPort == 9999)
    testFreeAddress.close()