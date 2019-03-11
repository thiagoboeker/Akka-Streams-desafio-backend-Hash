package products

import java.time.LocalDate
import java.util.Date

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.Flow
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
object UserDomain {
   type UserID = String
   case class User(id: UserID, firstName: String, lastName: String, dateOfBirth: LocalDate)
   case class GetUserByID(id: UserID)
   def GetUser: (ActorRef) => Flow[UserID, User, NotUsed] = (actor) => {
      implicit val timeout = Timeout(2.seconds)
      Flow[UserID].mapAsync(1){
         case id: UserID => (actor ? GetUserByID(id)).mapTo[User]
      }
   }
}
