package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

case class Start ()
case class Message (content:String)

class NetworkActor (val id : Int) extends Actor {

    val display = context.actorOf(Props[DisplayActor], name = "displayActor")
    val musician = context.actorOf(Props(new MusicianActor(self, id)), name = "musicianActor"+id)
    val heart = context.actorOf(Props(new HeartActor(id)), name = "heartActor"+id)
    val checker = context.actorOf(Props(new CheckerActor(self, id)), name = "checkerActor"+id)

    // Scheduler
    context.system.scheduler.schedule(0 milliseconds, 2000 milliseconds, checker, CheckOthersHearts)
    context.system.scheduler.schedule(0 milliseconds, 3000 milliseconds, checker, CheckSelfHeart)
    context.system.scheduler.scheduleOnce(5000 milliseconds, musician, CreationElection)

    def receive = {

        case Start => {
            display ! Message ("{Network}" + this.id + s" a été crée | Chemin : ${self.path}")
            display ! Message ("{Heart}" + this.id + s" a été crée | Chemin :  ${heart.path}")
        }

        case Message (content) => {
            display ! Message (content)
        }

        case Alive (n) => {
            musician ! Alive (n)
        }

        case Dead (n) => {
            musician ! Dead (n)
        }

    }
}
