package utilsTest

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import message.{Message, Output, PidMsg, Pong}
import utils.{ClientLauncher, Info}

import scala.collection.immutable.Queue

class TestUtils extends AnyFlatSpec:
  "A ClientLauncher" should "start launch a program via console command with arguments, intercept its output and send it a runtime input" in testClientLauncher()
  "An Info" should "update itself recursevly mantaining not-overriden informations" in testInfo()

  val testKit: ActorTestKit = ActorTestKit()
  val powershellCommand: String = if(System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"

  def testClientLauncher(): Unit =
    val probe = testKit.createTestProbe[Message]()
    val childProcessLauncher = ClientLauncher(9999, Queue(powershellCommand + " -ExecutionPolicy Bypass -File src/test/powershell/testClientLauncher.ps1", "test"), probe.ref, probe.ref)
    Thread(childProcessLauncher).start()
    probe.expectMessage(Output("test"))
    val stdin = childProcessLauncher.getChildProcessStdin
    Thread.sleep(5000)
    stdin.get.println("testInput")
    probe.expectMessage(Output("testInput"))

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


