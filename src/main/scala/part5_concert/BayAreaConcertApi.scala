package part5_concert

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

// ── Our simplified response model ──────────────────────────────────────────
case class Concert(
  name:     String,
  date:     String,
  time:     String,
  venue:    String,
  url:      String,
  imageUrl: String
)

// ── Ticketmaster API response models (only the fields we need) ─────────────
case class TmStartDate(localDate: String, localTime: Option[String])
case class TmDates(start: TmStartDate)
case class TmVenue(name: String)
case class TmEventEmbedded(venues: List[TmVenue])
case class TmImage(url: String, ratio: Option[String])
case class TmEvent(
  name:     String,
  url:      String,
  dates:    TmDates,
  images:   List[TmImage],
  embedded: Option[TmEventEmbedded]   // JSON key "_embedded"
)
case class TmResponseEmbedded(events: List[TmEvent])
case class TmResponse(embedded: Option[TmResponseEmbedded])   // JSON key "_embedded"

// ── JSON protocol ──────────────────────────────────────────────────────────
trait ConcertJsonProtocol extends DefaultJsonProtocol {
  // Ticketmaster formats – declared in dependency order
  implicit val tmStartDateFormat: RootJsonFormat[TmStartDate]           = jsonFormat2(TmStartDate)
  implicit val tmDatesFormat: RootJsonFormat[TmDates]                   = jsonFormat1(TmDates)
  implicit val tmVenueFormat: RootJsonFormat[TmVenue]                   = jsonFormat1(TmVenue)
  implicit val tmEventEmbeddedFormat: RootJsonFormat[TmEventEmbedded]   = jsonFormat1(TmEventEmbedded)
  implicit val tmImageFormat: RootJsonFormat[TmImage]                   = jsonFormat2(TmImage)

  // Use explicit field names to map Scala field `embedded` → JSON key `_embedded`
  implicit val tmEventFormat: RootJsonFormat[TmEvent] =
    jsonFormat(TmEvent, "name", "url", "dates", "images", "_embedded")

  implicit val tmResponseEmbeddedFormat: RootJsonFormat[TmResponseEmbedded] =
    jsonFormat1(TmResponseEmbedded)

  implicit val tmResponseFormat: RootJsonFormat[TmResponse] =
    jsonFormat(TmResponse, "_embedded")

  // Our simplified concert format
  implicit val concertFormat: RootJsonFormat[Concert] = jsonFormat6(Concert)
}

// ── Server ─────────────────────────────────────────────────────────────────
object BayAreaConcertApi extends App with ConcertJsonProtocol with SprayJsonSupport {

  implicit val system: ActorSystem        = ActorSystem("BayAreaConcertApi")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val apiKey: String = system.settings.config.getString("ticketmaster.api-key")

  def fetchConcerts(): Future[List[Concert]] = {
    val uri = Uri("https://app.ticketmaster.com/discovery/v2/events.json")
      .withQuery(Uri.Query(
        "apikey"             -> apiKey,
        "dmaId"              -> "306",   // San Francisco Bay Area DMA
        "classificationName" -> "music",
        "size"               -> "20",
        "sort"               -> "date,asc"
      ))

    Http().singleRequest(HttpRequest(uri = uri))
      .flatMap(response => Unmarshal(response.entity).to[TmResponse])
      .map { tmResponse =>
        val events = tmResponse.embedded.map(_.events).getOrElse(Nil)
        events.map { event =>
          val venue    = event.embedded.flatMap(_.venues.headOption).map(_.name).getOrElse("TBD")
          val imageUrl = event.images
            .find(_.ratio.contains("16_9"))
            .orElse(event.images.headOption)
            .map(_.url)
            .getOrElse("")
          Concert(
            name     = event.name,
            date     = event.dates.start.localDate,
            time     = event.dates.start.localTime.getOrElse("TBD"),
            venue    = venue,
            url      = event.url,
            imageUrl = imageUrl
          )
        }
      }
  }

  val route =
    pathPrefix("api") {
      path("concerts") {
        get {
          onComplete(fetchConcerts()) {
            case Success(concerts) => complete(concerts)
            case Failure(ex)       => complete(StatusCodes.InternalServerError -> ex.getMessage)
          }
        }
      }
    }

  Http().bindAndHandle(route, "localhost", 8080)
  println("Bay Area Concert API running → http://localhost:8080/api/concerts")
}
