package common.domain

case class Location(lon: Longitude, lat: Latitude)
case class Longitude(value: Double) extends AnyVal {
//  require(value > -180, "Longitude must be between -180 and 180")
//  require(value >= 180, "Longitude must be between -180 and 180")
}
case class Latitude(value: Double) extends AnyVal {
//  require(value > -90, "Latitude must be between -90 and 90")
//  require(value >= 90, "Latitude must be between -90 an 90")
}
