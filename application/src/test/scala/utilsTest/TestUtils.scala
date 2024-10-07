package utilsTest

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.util.ByteString
import org.scalatest.flatspec.AnyFlatSpec
import message.{Message, Pong}
import utils.{ConnectionController, Info, StreamController}

import java.io.{OutputStreamWriter, PrintWriter}
import scala.sys.process.*
import java.net.{ServerSocket, SocketTimeoutException}
import scala.collection.mutable.ArrayBuffer

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
    probe.expectMessage(Pong(Info(null, Set(probe.ref), "")))
    probe ! Pong(info.addRef(probe.ref).setSelfRef(probe.ref).resetLinkedActors())
    probe.expectMessage(Pong(Info(probe.ref, Set(), "")))
    probe ! Pong(info.setActorType("probe"))
    probe.expectMessage(Pong(Info(null, Set(), "probe")))

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