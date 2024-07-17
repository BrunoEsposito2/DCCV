package actorsBehavior

import actor.ReachableActor
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.testkit.TestProbe
import message.{Message, Ping, Pong}
import utils.{Info, TestProcessManager}

class TestBehavior extends AnyFlatSpec:

  "A ReachableActor" should "be reachable" in testPong()
  "A ProcessManager" should "return the console output of a given command" in testProcessManager()

  val testKit: ActorTestKit = ActorTestKit()


  def testPong(): Unit =
    val pinger = testKit.createTestProbe[Message]()
    val exampleInfo = Info()
    val actorRef = testKit.spawn(ReachableActor(exampleInfo).behavior())
    actorRef ! Ping(pinger.ref)
    pinger.expectMessage(Pong(exampleInfo))

  def testProcessManager(): Unit =
    println(TestProcessManager().execute())
    assert(TestProcessManager().execute().replaceAll("\n", "").replaceAll("\r", "").equals("Command executed successfully: test99"))




