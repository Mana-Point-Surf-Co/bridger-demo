package com.bridger.job.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class GeoJsonToKmlConverter(
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    fun convertToKml(geoJsonString: String): String {
        val geoJson = objectMapper.readTree(geoJsonString)

        val kmlBuilder = StringBuilder()
        kmlBuilder.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        kmlBuilder.append("""<kml xmlns="http://www.opengis.net/kml/2.2">""").append("\n")
        kmlBuilder.append("  <Document>\n")

        when (geoJson.get("type").asText()) {
            "Feature" -> {
                processFeature(geoJson, kmlBuilder)
            }
            "FeatureCollection" -> {
                geoJson.get("features").forEach { feature ->
                    processFeature(feature, kmlBuilder)
                }
            }
            "Point", "LineString", "Polygon" -> {
                // Direct geometry without Feature wrapper
                processGeometry(geoJson, kmlBuilder, "Unnamed")
            }
            else -> {
                logger.warn { "Unknown GeoJSON type: ${geoJson.get("type").asText()}" }
            }
        }

        kmlBuilder.append("  </Document>\n")
        kmlBuilder.append("</kml>")

        return kmlBuilder.toString()
    }

    private fun processFeature(feature: JsonNode, kmlBuilder: StringBuilder) {
        val geometry = feature.get("geometry")
        val properties = feature.get("properties")
        val name = properties?.get("name")?.asText() ?: "Unnamed"

        processGeometry(geometry, kmlBuilder, name)
    }

    private fun processGeometry(geometry: JsonNode, kmlBuilder: StringBuilder, name: String) {
        val geometryType = geometry.get("type").asText()
        val coordinates = geometry.get("coordinates")

        when (geometryType) {
            "Point" -> {
                kmlBuilder.append("    <Placemark>\n")
                kmlBuilder.append("      <name>").append(escapeXml(name)).append("</name>\n")
                kmlBuilder.append("      <Point>\n")
                kmlBuilder.append("        <coordinates>")
                    .append(pointToKmlCoordinates(coordinates))
                    .append("</coordinates>\n")
                kmlBuilder.append("      </Point>\n")
                kmlBuilder.append("    </Placemark>\n")
            }
            "LineString" -> {
                kmlBuilder.append("    <Placemark>\n")
                kmlBuilder.append("      <name>").append(escapeXml(name)).append("</name>\n")
                kmlBuilder.append("      <LineString>\n")
                kmlBuilder.append("        <coordinates>\n")
                kmlBuilder.append("          ").append(lineStringToKmlCoordinates(coordinates)).append("\n")
                kmlBuilder.append("        </coordinates>\n")
                kmlBuilder.append("      </LineString>\n")
                kmlBuilder.append("    </Placemark>\n")
            }
            "Polygon" -> {
                kmlBuilder.append("    <Placemark>\n")
                kmlBuilder.append("      <name>").append(escapeXml(name)).append("</name>\n")
                kmlBuilder.append("      <Polygon>\n")
                kmlBuilder.append("        <outerBoundaryIs>\n")
                kmlBuilder.append("          <LinearRing>\n")
                kmlBuilder.append("            <coordinates>\n")
                kmlBuilder.append("              ").append(polygonToKmlCoordinates(coordinates)).append("\n")
                kmlBuilder.append("            </coordinates>\n")
                kmlBuilder.append("          </LinearRing>\n")
                kmlBuilder.append("        </outerBoundaryIs>\n")
                kmlBuilder.append("      </Polygon>\n")
                kmlBuilder.append("    </Placemark>\n")
            }
            else -> {
                logger.warn { "Unsupported geometry type: $geometryType" }
            }
        }
    }

    fun pointToKmlCoordinates(coordinates: JsonNode): String {
        // GeoJSON: [lon, lat, alt?]
        // KML: lon,lat,alt
        val lon = coordinates[0].asDouble()
        val lat = coordinates[1].asDouble()
        val alt = if (coordinates.size() > 2) coordinates[2].asDouble() else 0.0
        return "$lon,$lat,$alt"
    }

    fun lineStringToKmlCoordinates(coordinates: JsonNode): String =
        coordinates.joinToString(" ") { coord ->
            val lon = coord[0].asDouble()
            val lat = coord[1].asDouble()
            val alt = if (coord.size() > 2) coord[2].asDouble() else 0.0
            "$lon,$lat,$alt"
        }


    // Just the outer ring.
    fun polygonToKmlCoordinates(coordinates: JsonNode): String {
        val outerRing = coordinates[0]
        return outerRing.joinToString(" ") { coord ->
            val lon = coord[0].asDouble()
            val lat = coord[1].asDouble()
            val alt = if (coord.size() > 2) coord[2].asDouble() else 0.0
            "$lon,$lat,$alt"
        }
    }

    fun escapeXml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

}
