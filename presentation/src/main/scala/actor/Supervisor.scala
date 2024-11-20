package actor

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import message.{CameraMap, ChildStatus, GetChildStatus, GetInputs, GetOutputs, InputServiceMsg, InputsListing, Message, OutputListing, Pong}
import utils.{ChildStatuses, Info}

import scala.collection.immutable.HashMap
import scala.concurrent.duration.{FiniteDuration, SECONDS}


object Supervisor:
  def apply(): Supervisor = new Supervisor()

class Supervisor:
  def create(): Behavior =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { (timers: TimerScheduler[Message]) =>
        implicit val context: ActorContext[Message] = ctx
        val inputsKey = ServiceKey[Message]("inputs")
        val outputsKey = ServiceKey[Message]("outputs")
        timers.startTimerWithFixedDelay("inputsTimer", GetInputs(), FiniteDuration(3, SECONDS))
        timers.startTimerWithFixedDelay("outputsTimer", GetOutputs(), FiniteDuration(5, SECONDS))
        implicit val listingResponseAdapter: ActorRef[Receptionist.Listing] =
          context.messageAdapter[Receptionist.Listing] {
            case inputsKey.Listing(listings) => InputsListing(listings)
            case outputsKey.Listing(listings) => OutputListing(listings)
          }
        context.system.receptionist.tell(Receptionist.register(ServiceKey[InputServiceMsg]("supervisor"), context.self))
        Supervisor().behavior(HashMap())
      }
    }

  private def behavior(camerasStatus:HashMap[Info, ChildStatuses])(implicit ctx: ActorContext[Message], adapter: ActorRef[Receptionist.Listing]): Behavior =
    Behaviors.setup(ctx => {
      Behaviors.receiveMessage {
        case GetOutputs() =>
          val outputsKey = ServiceKey[Message]("outputs")
          ctx.system.receptionist.tell(Receptionist.find(outputsKey, adapter))
          Supervisor().behavior(camerasStatus)

        case GetInputs() =>
          val inputsKey = ServiceKey[Message]("inputs")
          ctx.system.receptionist.tell(Receptionist.find(inputsKey, adapter))
          Supervisor().behavior(camerasStatus)

        case OutputListing(listing) =>
          listing.foreach(_ ! CameraMap(ctx.self, camerasStatus))
          Supervisor().behavior(camerasStatus)

        case Pong(info) =>
          Supervisor().behavior(camerasStatus)

        case InputsListing(listing) =>
          listing.foreach(_ ! GetChildStatus(ctx.self))
          Supervisor().behavior(camerasStatus.filter((k, v) => listing.contains(k.self)))

        case ChildStatus(info, status) =>
          Supervisor().behavior(camerasStatus + (info -> status))
      }
    })