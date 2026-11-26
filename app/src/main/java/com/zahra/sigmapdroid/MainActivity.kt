package com.zahra.sigmapdroid

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zahra.sigmapdroid.databinding.ActivityMainBinding
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private var maplibreMap: MapLibreMap? = null

    // ULBI Campus Coordinates (Bandung)
    private val campusLocation = LatLng(-6.8732, 107.5758)
    private val defaultZoom = 16.5

    private val styles = listOf(
        "OpenStreetMap" to createOsmRasterStyle(),
        "Carto Positron" to createCartoStyle(),
        "MapLibre Demo" to "https://demotiles.maplibre.org/style.json"
    )
    private var currentStyleIndex = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            enableLocationComponent()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk pelacakan GPS.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapLibre
        MapLibre.getInstance(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        setupButtons()
    }

    private fun setupButtons() {
        // Recenter to ULBI Campus
        binding.fabRecenter.setOnClickListener {
            maplibreMap?.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(campusLocation)
                        .zoom(defaultZoom)
                        .tilt(30.0)
                        .build()
                ),
                1200
            )
        }

        // Switch Basemap
        binding.fabStyle.setOnClickListener {
            currentStyleIndex = (currentStyleIndex + 1) % styles.size
            val (name, styleDefinition) = styles[currentStyleIndex]
            Toast.makeText(this, "Basemap: $name", Toast.LENGTH_SHORT).show()
            applyStyle(styleDefinition)
        }

        // GPS My Location
        binding.fabLocation.setOnClickListener {
            checkAndEnableLocation()
        }

        // About Button
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.cardFeatureInfo.setOnClickListener {
            binding.cardFeatureInfo.visibility = View.GONE
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        maplibreMap = map

        map.cameraPosition = CameraPosition.Builder()
            .target(campusLocation)
            .zoom(defaultZoom)
            .build()

        applyStyle(styles[currentStyleIndex].second)

        // Feature Click Listener
        map.addOnMapClickListener { point ->
            val pixel = map.projection.toScreenLocation(point)
            val features = map.queryRenderedFeatures(
                pixel,
                "campus-buildings-fill",
                "campus-points-circle",
                "campus-roads-line"
            )

            if (features.isNotEmpty()) {
                val feature = features[0]
                val name = feature.getStringProperty("nama")
                    ?: feature.getStringProperty("Gedung")
                    ?: feature.getStringProperty("Gedung ")
                    ?: feature.getStringProperty("Sekre")
                    ?: feature.getStringProperty("gerbang ")
                    ?: feature.getStringProperty("Jalan ")
                    ?: "Objek Spasial Kampus"

                val category = feature.getStringProperty("kategori")
                    ?: if (feature.geometry()?.type() == "Polygon") "Gedung / Bangunan"
                    else if (feature.geometry()?.type() == "LineString") "Jalan / Rute"
                    else "Titik POI"

                binding.tvFeatureName.text = name
                binding.tvFeatureDetails.text = "Kategori: $category | LatLng: ${String.format("%.5f, %.5f", point.latitude, point.longitude)}"
                binding.cardFeatureInfo.visibility = View.VISIBLE
                true
            } else {
                binding.cardFeatureInfo.visibility = View.GONE
                false
            }
        }
    }

    private fun applyStyle(styleJsonOrUrl: String) {
        val styleBuilder = if (styleJsonOrUrl.startsWith("http")) {
            Style.Builder().fromUri(styleJsonOrUrl)
        } else {
            Style.Builder().fromJson(styleJsonOrUrl)
        }

        maplibreMap?.setStyle(styleBuilder) { style ->
            addCampusGeoJsonLayer(style)
            if (hasLocationPermission()) {
                enableLocationComponent()
            }
        }
    }

    private fun addCampusGeoJsonLayer(style: Style) {
        try {
            val geoJsonString = assets.open("kampus_ulbi.geojson").bufferedReader().use { it.readText() }
            val sourceId = "campus-ulbi-source"

            if (style.getSource(sourceId) == null) {
                style.addSource(GeoJsonSource(sourceId, geoJsonString))
            }

            // 1. Polygon Fill Layer (Bangunan)
            if (style.getLayer("campus-buildings-fill") == null) {
                val fillLayer = FillLayer("campus-buildings-fill", sourceId).apply {
                    setFilter(eq(geometryType(), literal("Polygon")))
                    setProperties(
                        fillColor(
                            coalesce(
                                get("fill"),
                                literal("#0284c7")
                            )
                        ),
                        fillOpacity(0.65f)
                    )
                }
                style.addLayer(fillLayer)
            }

            // 2. Polygon Outline Layer
            if (style.getLayer("campus-buildings-outline") == null) {
                val outlineLayer = LineLayer("campus-buildings-outline", sourceId).apply {
                    setFilter(eq(geometryType(), literal("Polygon")))
                    setProperties(
                        lineColor(
                            coalesce(
                                get("stroke"),
                                literal("#0f172a")
                            )
                        ),
                        lineWidth(2f)
                    )
                }
                style.addLayer(outlineLayer)
            }

            // 3. Roads Line Layer
            if (style.getLayer("campus-roads-line") == null) {
                val lineLayer = LineLayer("campus-roads-line", sourceId).apply {
                    setFilter(eq(geometryType(), literal("LineString")))
                    setProperties(
                        lineColor(
                            coalesce(
                                get("stroke"),
                                literal("#3b82f6")
                            )
                        ),
                        lineWidth(3.5f)
                    )
                }
                style.addLayer(lineLayer)
            }

            // 4. Points Layer (POI / Gerbang / Fasilitas)
            if (style.getLayer("campus-points-circle") == null) {
                val circleLayer = CircleLayer("campus-points-circle", sourceId).apply {
                    setFilter(eq(geometryType(), literal("Point")))
                    setProperties(
                        circleRadius(8f),
                        circleColor(
                            coalesce(
                                get("marker-color"),
                                literal("#e11d48")
                            )
                        ),
                        circleStrokeColor(Color.WHITE),
                        circleStrokeWidth(2.5f)
                    )
                }
                style.addLayer(circleLayer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndEnableLocation() {
        if (hasLocationPermission()) {
            enableLocationComponent()
            val location = maplibreMap?.locationComponent?.lastKnownLocation
            if (location != null) {
                maplibreMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        17.0
                    )
                )
            } else {
                Toast.makeText(this, "Mencari koordinat GPS perangkat...", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent() {
        val style = maplibreMap?.style ?: return
        if (!hasLocationPermission()) return

        val locationComponent = maplibreMap?.locationComponent ?: return
        val activationOptions = LocationComponentActivationOptions.builder(this, style)
            .useDefaultLocationEngine(true)
            .build()

        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.COMPASS
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Tentang SIG Mapdroid")
            .setMessage(
                "Aplikasi Android Sistem Informasi Geografis (SIG) berbasis MapLibre Native SDK.\n\n" +
                "👤 Pengembang: Zahra Ramanayshilla\n" +
                "🎓 Mata Kuliah: SIG Semester 5\n" +
                "🏫 Kampus: ULBI Bandung\n" +
                "🗺️ Engine Peta: MapLibre.org\n" +
                "⚡ CI/CD: GitHub Actions Auto Tag & Release"
            )
            .setPositiveButton("Tutup") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Styles definitions
    private fun createOsmRasterStyle(): String {
        return """
        {
          "version": 8,
          "sources": {
            "osm-raster": {
              "type": "raster",
              "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
              "tileSize": 256,
              "attribution": "© OpenStreetMap contributors"
            }
          },
          "layers": [
            {
              "id": "osm-tiles",
              "type": "raster",
              "source": "osm-raster",
              "minzoom": 0,
              "maxzoom": 19
            }
          ]
        }
        """.trimIndent()
    }

    private fun createCartoStyle(): String {
        return """
        {
          "version": 8,
          "sources": {
            "carto-raster": {
              "type": "raster",
              "tiles": [
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png"
              ],
              "tileSize": 256,
              "attribution": "© CARTO"
            }
          },
          "layers": [
            {
              "id": "carto-tiles",
              "type": "raster",
              "source": "carto-raster",
              "minzoom": 0,
              "maxzoom": 19
            }
          ]
        }
        """.trimIndent()
    }

    // Lifecycle
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
}
