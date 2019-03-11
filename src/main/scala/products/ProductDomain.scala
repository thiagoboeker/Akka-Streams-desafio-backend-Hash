package products

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.Flow
import akka.pattern.ask
import akka.util.Timeout
import products.DiscountDomain.Discount

import scala.concurrent.duration._

object ProductDomain {
   
   type ProductID = String
   
   case class Product(id: String, title: String, description: String, priceInCents: Int, implicit val discount: Discount = Discount(0,0))
   case class GetProductByID(id: ProductID)
   
   def GetProduct: (ActorRef) => Flow[ProductID, Product, NotUsed] = (actor) => {
      implicit val timeout = Timeout(2.seconds)
      Flow[ProductID].mapAsync(1)({ // This map async will get buffered because the ask inside it.
         case id: ProductID => (actor ? GetProductByID(id)).mapTo[Product]
      })
   }
}
