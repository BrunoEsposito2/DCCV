package actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import message.{Message, Ping}
import routing.VertxRouter

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object Server:
  def apply(): Server = new Server()

private class Server:
  private val vertxRouter = VertxRouter()
  private var listingResponseAdapter: ActorRef[Receptionist.Listing] = _

  // TODO(delete): to be inserted in a specific Message data type in order to be used in receiveMessage
  private val receptionistListing = Receptionist.Listing

  def behavior(): Behavior[Message] =
    Behaviors.setup { ctx =>
      // TODO: modify ServiceKey "id" with appropriate one
      ctx.system.receptionist.tell(Receptionist.register(ServiceKey[Message]("id"), ctx.self))

      // TODO: modify Ping type message with appropriate one
      listingResponseAdapter = ctx.messageAdapter[Receptionist.Listing](listing => Ping(ctx.self))

      Behaviors.withTimers { t =>
        t.startTimerAtFixedRate(Ping(ctx.self), FiniteDuration(2000, MILLISECONDS))

        // TODO: modify message receive with type message that includes a variabile as receptionistListing
        //       and handle it appropriately (e.g. with useful ServiceKey(s) and Set[ActorRef[Message]])
        Behaviors.receiveMessage {
          case Ping(replyTo) =>
            receptionistListing(ServiceKey[Message]("2"), Set(ctx.self))
              .serviceInstances(ServiceKey[Message]("1"))
                .foreach(el => {
                  el.tell(Ping(replyTo))
                })
            Behaviors.same
        }
      }
    }