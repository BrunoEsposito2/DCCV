package actor

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import message.{Config, ConfigServiceSuccess, InputServiceMsg, Message}

import scala.collection.immutable.Queue
import scala.concurrent.duration.{FiniteDuration, SECONDS}

case class ConfigCameraManager(replyTo:ActorRef[Message], cameraRef: ActorRef[Message], args:Queue[String]) extends InputServiceMsg
case class GetOutputRef() extends InputServiceMsg
case class OutputServiceListing(listings:Set[ActorRef[Message]]) extends InputServiceMsg

object ConfigManager:
  def apply(outputRef: ActorRef[Message]):ConfigManager = new ConfigManager(outputRef)

private class ConfigManager(outputRef: ActorRef[Message]):

  def create(): Behavior =
    Behaviors.withTimers { (timers: TimerScheduler[Message]) =>
      Behaviors.setup { ctx =>
        implicit val context: ActorContext[Message] = ctx
        timers.startTimerWithFixedDelay("outputServiceRefTimer", GetOutputRef(), FiniteDuration(60, SECONDS))
        val outputServiceKey = ServiceKey[Message]("outputService")
        implicit val listingResponseAdapter: ActorRef[Receptionist.Listing] =
          ctx.messageAdapter[Receptionist.Listing] {
            case outputServiceKey.Listing(listings) => OutputServiceListing(listings)
          }
        context.system.receptionist.tell(Receptionist.register(ServiceKey[InputServiceMsg]("configService"), context.self))
        ConfigManager(outputRef).behavior
      }
    }

  def behavior(implicit ctx:ActorContext[Message], adapter: ActorRef[Receptionist.Listing]): Behavior =
    Behaviors.setup(ctx => {
      Behaviors.receiveMessage {
        case ConfigCameraManager(replyTo, cameraRef, args) =>
          cameraRef ! Config(outputRef, args)
          ConfigManager(outputRef).behavior

        case ConfigServiceSuccess(author, sourceRef) =>
          ConfigManager(outputRef).behavior

        case OutputServiceListing(listings) =>
          ConfigManager(listings.head).behavior
      }
    })


