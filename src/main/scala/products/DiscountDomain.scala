package products

import java.time.LocalDate
import akka.NotUsed
import akka.stream.scaladsl.Flow
import products.ProductDomain.Product
import products.UserDomain.User

object DiscountDomain {
   case class Discount(pct: Double, ValueInCents: Int)
   val BlackFridayDay = 25
   val blackFridayMonth = 11
   def GetDiscount:Flow[(User, Product), Product, NotUsed]  = {
      Flow[(User, Product)].map {
         case (user: User, prod: Product) => {
            
            // The business Logic, will return a Product with a discount
            
            val ForBlackFriday = 0
            val ForBirthday = 1
            
            val today = LocalDate.now()
            val isBirthday = today.equals(LocalDate.of(today.getYear, user.dateOfBirth.getMonth, user.dateOfBirth.getDayOfMonth))
            val isBlackFriday = today.equals(LocalDate.of(today.getYear, blackFridayMonth, BlackFridayDay))
            
            val discountList = Seq(0.10, 0.05)
            val discountValues = discountList.map(pct => (pct * prod.priceInCents).toInt)
            val prices = discountValues.map(x => (prod.priceInCents - x).toInt)
            
            if(isBlackFriday) {
               val discount = Discount(discountList(ForBlackFriday), discountValues(ForBlackFriday))
               Product(prod.id, prod.title, prod.description, prices(ForBlackFriday), discount)
            }else
            
            if(isBirthday) {
               val discount = Discount(discountList(ForBirthday), discountValues(ForBirthday))
               Product(prod.id, prod.title, prod.description, prices(ForBirthday), discount)
            }
            else Product(prod.id, prod.title, prod.description, prod.priceInCents, Discount(0.0, 0))
         }
      }
   }
}
