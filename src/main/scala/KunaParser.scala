import akka.actor.{Actor, ActorSystem, Props}
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import org.json4s._
import org.json4s.native.JsonMethods._
import scalikejdbc._
import org.joda.time._

import scala.concurrent.duration._
import scala.io.Source
import scala.language.implicitConversions
import KunaParser.tradesWriter
import JDBConn.session


case object JDBConn {
  Class.forName("com.mysql.jdbc.Driver")
  // root/password123
  ConnectionPool.singleton("jdbc:mysql://172.17.0.1:3306/kuna", "scala", "scalapass")
  implicit val session: AutoSession.type = AutoSession
}

case object Tick

case class TradeHelper(id: Long, price: String, volume: String, funds: String,
                       market: String, created_at: String, side: String) {
  def toTrade = Trade(id, price.toDouble, volume.toDouble, funds.toDouble, market, DateTime.parse(created_at), side)
}

case class Trade(id: Long, price: Double, volume: Double, funds: Double, market: String, created_at: DateTime,
                 side: String)

object Trade extends SQLSyntaxSupport[Trade] {
  override val tableName = "trades"

  override def columns = Seq("id", "price", "volume", "funds", "market", "created_at", "side")

  def apply(rs: WrappedResultSet) = new Trade(rs.long("id"), rs.double("price"), rs.double("volume"),
    rs.double("funds"), rs.string("market"), rs.jodaDateTime("created_at"), rs.string("side"))
}

class TradesPulling extends Actor {

  def receive = {
    case Tick => println("TradesPulling: 30 Sec passed")
      import java.net.URL

      val kunaURL = "https://kuna.io/api/v2/trades?market=btcuah"
      val requestProperties = Map(
        "User-Agent" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0"
      )
      val connection = new URL(kunaURL).openConnection
      requestProperties.foreach({
        case (name, value) => connection.setRequestProperty(name, value)
      })

      implicit val formats: Formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

      val json = parse(Source.fromInputStream(connection.getInputStream).mkString).extract[Array[TradeHelper]]
      val jsonMap: Array[Trade] = json.map(_.toTrade)
      jsonMap.foreach(trade => tradesWriter ! trade)

  }
}

class TradesWriter extends Actor {

  def receive = {
    case Trade(id, price, volume, funds, market, created_at, side) =>
      val t = Trade.column
      try {
        sql"""insert into ${Trade.table} (${t.id}, ${t.price}, ${t.volume}, ${t.funds}, ${t.market}, ${t.created_at}, ${t.side})
    values ($id, $price, $volume, $funds, $market, $created_at, $side)""".update.apply()
        println("Trade saved")
      } catch {
        case constraint: MySQLIntegrityConstraintViolationException => println("TradesWriter: Duplicate Entry")
        case e: Exception => println(s"TradesWriter: Unhandled exception: ${e}")
      }
  }
}

object KunaParser extends App {

  import system.dispatcher

  val system = ActorSystem("Akka-Scheduler")
  val ticker = system.actorOf(Props[TradesPulling])
  val tradesWriter = system.actorOf(Props[TradesWriter])

  system.scheduler.schedule(0 milliseconds, 30 second, ticker, Tick)


}
