package upmc.akka.leader

import akka.actor.{Props,  Actor,  ActorRef,  ActorSystem}

case class Beating (n:Int)

class HeartActor (val id : Int) extends Actor {

    def receive = {
        case Check => {
            sender() ! Beating (id)
        }
    }

}

