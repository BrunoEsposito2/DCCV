package database

import actor.ReachableActor
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.ActorRef
import message.{Message, Ping, Pong}
import utils.Info

class MongoDBDriverTest extends AnyFlatSpec:

  "A ReachableActor" should "be reachable" in testPong()

  val testKit: ActorTestKit = ActorTestKit()


  def testPong(): Unit =
    val pinger = testKit.createTestProbe[Message]()
    val exampleInfo = Info()
    val actorRef = testKit.spawn(ReachableActor(exampleInfo).behavior())
    actorRef ! Ping(pinger.ref)
    pinger.expectMessage(Pong(exampleInfo))