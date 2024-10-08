package actorsBehavior

import actor.{CameraManager, ConfigCameraManager, ConfigManager, ReachableActor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import message.*
import org.scalatest.flatspec.AnyFlatSpec
import utils.{Info, InputServiceErrors, StandardChildProcessCommands}

import scala.collection.immutable.Queue
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

class TestBehavior extends AnyFlatSpec:

  "The ConfigManager" should "forward the public config message from a client to a CameraManager and let the client know its response" in testConfigManagerBehavior()

  def testConfigManagerBehavior(): Unit =
    val powershellCommand: String = if (System.getProperty("os.name").toLowerCase().contains("win")) "powershell" else "pwsh"
    val testKit: ActorTestKit = ActorTestKit()
    val clientProbe = testKit.createTestProbe[Message]()
    val cameraManager = testKit.spawn(CameraManager(9999).create())
    val configManager = testKit.spawn(ConfigManager(clientProbe.ref).create())
    configManager ! ConfigCameraManager(clientProbe.ref, cameraManager, Queue(powershellCommand + " -ExecutionPolicy Bypass -File ../application/src/test/powershell/testCameraManagerScript.ps1 ", "run1"))
    clientProbe.expectMessageType[ConfigServiceSuccess]
    cameraManager ! Input(clientProbe.ref, StandardChildProcessCommands.Kill.command)
    clientProbe.expectMessage(InputServiceSuccess(Info().setSelfRef(cameraManager).setActorType("CameraManager")))

    






