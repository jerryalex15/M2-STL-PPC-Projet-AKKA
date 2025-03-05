package upmc.akka.leader

import akka.actor._

case class MidiNote(note: Int)
case class Measure(chordList: List[Int])

class Player extends Actor {
  def receive = {
    case MidiNote(note) => {
      // Play a single note (simulated)
      context.parent ! Message(s"Playing note: $note")
    }

    case Measure(chordList) => {
      // Play a measure (sequence of notes)
      context.parent ! Message(s"Playing measure with chords: ${chordList.mkString(", ")}")

      // Simulate playing each note in the measure
      chordList.foreach(note => {
        Thread.sleep(200) // Simulate note duration
        self ! MidiNote(note)
      })
    }
  }
}

