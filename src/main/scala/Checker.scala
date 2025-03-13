package upmc.akka.leader

import akka.actor.{Props,  Actor,  ActorRef,  ActorSystem, ActorSelection}

case class Check ()
case class CheckOthersHearts ()
case class CheckSelfHeart ()
case class Alive (n:Int)
case class Dead (n:Int)

class CheckerActor (node : ActorRef,  val id : Int) extends Actor {

    // Adresses des coeurs
    var heartsAddress = new Array[ActorSelection](4) 
    // Newtorks vivants
    var networksAlive = new Array[Boolean](4)
    // Temps sans réponse pour chaque coeurs
    var noResponseHeartsCount = new Array[Int](4) 
    
    // Init
    networksAlive(id) = true
    noResponseHeartsCount(id) = 0

    // Init pour les autres network
    for(i <- 0 to 3 ) {
        try {
            heartsAddress(i) = context.actorSelection("akka.tcp://LeaderSystem" + i + "@127.0.0.1:600" + i + "/user/node" + i + "/heartActor" + i)
        } catch {
            case e : Throwable => println(s"[Error] {Checker} Pas de communication avec le musicien $i: ${e.getMessage}")
        }
        if (i != id) {
            networksAlive(i) = false
            noResponseHeartsCount(i) = 0
        }
    }
    
    def receive = {

        case CheckSelfHeart => {
            for(i <- 0 to 3 if i != id) {
                noResponseHeartsCount(i) = noResponseHeartsCount(i) + 1

                noResponseHeartsCount(i) match {
                    case 2 => // Pas de réponse, on attend encore
                        node ! Message ("{Checker} Pas de réponse du Network " + i +", en attente...")
                    case 4 => // Pas de réponse depuis longtemps, on le déclare mort
                        networksAlive(i) = false
                        node ! Dead (i)
                    case _ => null
                }
            }
        }

        case CheckOthersHearts => {
            for(i <- 0 to 3 if i != id) {
                heartsAddress(i) ! Check
            }
        }

        // Réponse d'un coeur
        case Beating (n) => {
            noResponseHeartsCount(n) = 0
            networksAlive(n) = true
            node ! Message ("{Checker} Coeur" + n + " est vivant")
            node ! Alive (n)
        }

    }

}
