package upmc.akka.leader

import akka.actor._
import scala.util.Random

case class GetMeasure(nb: Int)

class Provider extends Actor {
  val database = context.actorOf(Props[Database], "database")
  val random = new Random()

  def receive = {
    case GetMeasure(nb) => {
      // Request measures from the database
      database ! GetMeasure(nb)
    }

    case measure: Measure => {
      // Forward the measure to the parent (Musician)
      context.parent ! measure
    }
  }
}

