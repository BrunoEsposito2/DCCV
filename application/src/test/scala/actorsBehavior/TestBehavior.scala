package actorsBehavior

import actor.ReachableActor
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.testkit.TestProbe
import message.{Message, Ping, Pong}
import utils.Info

class TestBehavior extends AnyFlatSpec:

  "A ReachableActor" should "be reachable" in testPong()

  val testKit: ActorTestKit = ActorTestKit()


  def testPong(): Unit =
    val pinger = testKit.createTestProbe[Message]()
    val exampleInfo = new Info();
    val actorRef = testKit.spawn(ReachableActor(exampleInfo).behavior())
    actorRef ! new Ping(pinger.ref)
    pinger.expectMessage(new Pong(exampleInfo))