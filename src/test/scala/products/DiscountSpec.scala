package products

import java.time.LocalDate
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import products.ProductDomain.Product
import products.UserDomain.User
import DiscountDomain._
import akka.stream.testkit.scaladsl.TestSink

class DiscountSpec extends TestKit(ActorSystem("DiscountSpec")) with WordSpecLike with BeforeAndAfterAll{
   
   override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
   
   "A Discount component" should {
      "Provide the discount" in {
         implicit val mat = ActorMaterializer()
         
         // Some test data
         val users = Seq(
            User("2001", "Jhon", "Doe", LocalDate.of(1992, 3, 10)),
            User("2002", "Jhon", "Cena", LocalDate.of(1992, 4, 10)),
            User("2003", "Jhon", "Foster", LocalDate.of(1992, 5, 10))
         )
         val products = Seq(
            Product("3001", "Product 1", "A Product", 1000),
            Product("3002", "Product 2", "A Product", 1000),
            Product("3003", "Product 3", "A Product", 1000)
         )
         
         // This is the test Sink to do the asserts
         val probeSink = TestSink.probe[Product]
         
         // Zip the users with the products to generate List[(User, Product)]
         val simpleSource = Source(users.zip(products).toList)
         val materializedDiscounts = simpleSource.via(GetDiscount).alsoTo(Sink.foreach(println)).runWith(probeSink)
         
         // The assertion
         materializedDiscounts
           .request(3)
           .expectNext(
              Product("3001", "Product 1", "A Product", 950, Discount(0.05, 50)),
              Product("3002", "Product 2", "A Product", 1000, Discount(0, 0)),
              Product("3003", "Product 3", "A Product", 1000, Discount(0, 0))
           )
           .expectComplete()
      }
   }
   
}
