package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Map, Set}
import scala.util.Random

// Additional messages
case class LearnCurrConductorId(id: Int)
case object PlayConductor
case object GoodbyeWorld
case object Election
case object Elected

class Musicien(val id: Int, val terminaux: List[Terminal]) extends Actor {
     // The different actors in the system
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val checker = context.actorOf(Props(new Checker(id, terminaux)), name = "checker")
     val player = context.actorOf(Props[Player], name = "player")
     val provider = context.actorOf(Props[Provider], name = "provider")

     // State variables
     var isConductor = false
     var currentConductorId = -1
     val aliveMusicians = Set[Int]()
     val random = new Random()
     var waitingForMusicians = false
     var waitingTimer: Option[Cancellable] = None

     // If id is 0, this musician is the initial conductor
     if (id == 0) {
          isConductor = true
          currentConductorId = id

          // Wait for other musicians to join
          waitingForMusicians = true
          waitingTimer = Some(context.system.scheduler.scheduleOnce(30.seconds, self, GoodbyeWorld))

          displayActor ! Message(s"Musicien $id is the conductor and waiting for other musicians")
     }

     def receive = {
          // Initialization
          case Start => {
               displayActor ! Message(s"Musicien $id is created")
               aliveMusicians += id

               // If not the conductor, announce presence to others
               if (!isConductor) {
                    terminaux.foreach(t => {
                         if (t.id != id) {
                              val path = s"akka.tcp://MozartSystem${t.id}@${t.ip}:${t.port}/user/Musicien${t.id}"
                              try {
                                   val musicianRef = context.actorSelection(path)
                                   musicianRef ! Alive(id)
                              } catch {
                                   case _: Exception => // Ignore if can't reach
                              }
                         }
                    })
               }
          }

          // Handle alive musicians
          case Alive(musicianId) => {
               aliveMusicians += musicianId
               displayActor ! Message(s"Musicien $musicianId is alive")

               // If conductor and waiting for musicians, start playing
               if (isConductor && waitingForMusicians && aliveMusicians.size > 1) {
                    waitingForMusicians = false
                    waitingTimer.foreach(_.cancel())
                    displayActor ! Message(s"Conductor $id starts playing with ${aliveMusicians.size} musicians")
                    self ! PlayConductor
               }

               // Inform other musicians about the current conductor
               sender() ! LearnCurrConductorId(currentConductorId)
          }

          // Handle dead musicians
          case Dead(musicianId) => {
               if (aliveMusicians.contains(musicianId)) {
                    aliveMusicians -= musicianId
                    displayActor ! Message(s"Musicien $musicianId is dead")

                    // If the dead musician was the conductor, start election
                    if (musicianId == currentConductorId) {
                         displayActor ! Message(s"Conductor $musicianId is dead, starting election")
                         self ! Election
                    }

                    // If conductor and now alone, start waiting timer
                    if (isConductor && aliveMusicians.size == 1) {
                         waitingForMusicians = true
                         waitingTimer = Some(context.system.scheduler.scheduleOnce(30.seconds, self, GoodbyeWorld))
                         displayActor ! Message(s"Conductor $id is now alone, waiting for other musicians")
                    }
               }
          }

          // Learn who the current conductor is
          case LearnCurrConductorId(conductorId) => {
               currentConductorId = conductorId
               displayActor ! Message(s"Learned that Musicien $conductorId is the conductor")
          }

          // Play as conductor
          case PlayConductor if isConductor => {
               // Select a random musician to play a measure
               if (aliveMusicians.size > 1) {
                    val availableMusicians = aliveMusicians.toList.filter(_ != id)
                    val selectedMusician = availableMusicians(random.nextInt(availableMusicians.length))

                    // Get a measure from the provider
                    provider ! GetMeasure(1)

                    // Schedule next play after 1.8 seconds
                    context.system.scheduler.scheduleOnce(1800.milliseconds, self, PlayConductor)

                    // Send the measure to the selected musician
                    val path = s"akka.tcp://MozartSystem${selectedMusician}@${terminaux.find(_.id == selectedMusician).get.ip}:${terminaux.find(_.id == selectedMusician).get.port}/user/Musicien${selectedMusician}"
                    try {
                         val musicianRef = context.actorSelection(path)
                         displayActor ! Message(s"Conductor $id sending measure to Musicien $selectedMusician")
                    } catch {
                         case _: Exception => {
                              // If can't reach, play it ourselves
                              displayActor ! Message(s"Couldn't reach Musicien $selectedMusician, playing myself")
                         }
                    }
               }
          }

          // Handle measures from provider or other musicians
          case measure: Measure => {
               // Play the measure
               player ! measure
          }

          // Election process
          case Election => {
               // Simple election: the musician with the lowest ID becomes the conductor
               val lowestIdAlive = aliveMusicians.min

               if (lowestIdAlive == id) {
                    // I am the new conductor
                    isConductor = true
                    currentConductorId = id
                    displayActor ! Message(s"Musicien $id elected as the new conductor")

                    // Inform all other musicians
                    aliveMusicians.foreach(musicianId => {
                         if (musicianId != id) {
                              val terminal = terminaux.find(_.id == musicianId).get
                              val path = s"akka.tcp://MozartSystem${musicianId}@${terminal.ip}:${terminal.port}/user/Musicien${musicianId}"
                              try {
                                   val musicianRef = context.actorSelection(path)
                                   musicianRef ! Elected
                                   musicianRef ! LearnCurrConductorId(id)
                              } catch {
                                   case _: Exception => // Ignore if can't reach
                              }
                         }
                    })

                    // Start playing as conductor
                    self ! PlayConductor
               }
          }

          // Handle election result
          case Elected => {
               displayActor ! Message(s"Election completed, waiting for conductor ID")
          }

          // Handle end of performance
          case GoodbyeWorld => {
               if (waitingForMusicians) {
                    displayActor ! Message(s"Musicien $id waited 30 seconds but no other musicians joined, ending performance")
                    context.system.terminate()
               }
          }
     }
}

