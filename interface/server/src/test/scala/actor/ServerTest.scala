package actor

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.flatspec.AnyFlatSpec
import akka.actor.typed.{ActorRef, Behavior}

class ServerTest extends AnyFlatSpec:

  "A sample actor test" should "receive messages" in testMessage()

  val testKit: ActorTestKit = ActorTestKit()

  trait Message
  case class MessageExample(v: Int) extends Message

  val receiveBehavior: Behaviors.Receive[Message] = Behaviors.receive[Message] { (context, message) =>
    message match {
      case MessageExample(v: Int) =>
        println(s"Received $v")
        Behaviors.same
    }
  }

  def testMessage(): Unit =
    val probe = testKit.createTestProbe[Message]()
    val actorRef = testKit.spawn(receiveBehavior)
    actorRef ! MessageExample(10)

