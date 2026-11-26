/**
 * SIG Mapdroid - Landing Page Logic
 * Menggunakan CrootJS (jscroot) untuk integrasi GitHub API rilis terbaru
 * dan MapLibre GL JS untuk visualisasi peta web interaktif.
 */

// 1. Import helper dari CrootJS via jsDelivr CDN
import { getJSON } from "https://cdn.jsdelivr.net/gh/jscroot/lib@0.2.8/api.min.js";
import { setInner, onClick } from "https://cdn.jsdelivr.net/gh/jscroot/lib@0.2.8/element.min.js";

// Konfigurasi Repositori GitHub
const GITHUB_OWNER = "zahraramanayshilla";
const GITHUB_REPO = "SIG_Mapdroid";
const GITHUB_RELEASE_API = `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`;
const GITHUB_FALLBACK_URL = `https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases`;

// Inisialisasi Deteksi Rilis dengan CrootJS
function initReleaseTracker() {
  console.log("CrootJS: Mengambil informasi rilis terbaru dari GitHub API...");

  getJSON(GITHUB_RELEASE_API, (response) => {
    // Tangani kemungkinan respons error / rate limit / 404
    if (!response || response.status === 404 || response.message?.includes("API rate limit") || !response.tag_name) {
      handleNoReleaseYet();
      return;
    }

    handleReleaseSuccess(response);
  });
}

function handleReleaseSuccess(release) {
  const tagName = release.tag_name || "v1.0.0";
  const publishedDate = release.published_at 
    ? new Date(release.published_at).toLocaleDateString('id-ID', { day: 'numeric', month: 'long', year: 'numeric' })
    : "Baru saja";

  // Temukan aset APK
  const apkAsset = release.assets?.find(asset => asset.name.endsWith('.apk')) || release.assets?.[0];
  const downloadUrl = apkAsset?.browser_download_url || release.html_url || GITHUB_FALLBACK_URL;
  const fileSizeMb = apkAsset?.size 
    ? (apkAsset.size / (1024 * 1024)).toFixed(1) + " MB" 
    : "~15 MB";

  // Perbarui UI menggunakan CrootJS
  setInner("badge-version", tagName);
  setInner("badge-size", fileSizeMb);
  setInner("release-tag-display", tagName);
  setInner("release-date-display", publishedDate);
  setInner("release-size-display", fileSizeMb);
  setInner("release-status-text", "Rilis APK Siap Diunduh");

  // Perbarui semua tautan tombol download
  const downloadButtons = [
    document.getElementById("btn-download-hero"),
    document.getElementById("btn-download-card"),
    document.getElementById("btn-download-nav")
  ];

  downloadButtons.forEach(btn => {
    if (btn) {
      btn.href = downloadUrl;
      btn.setAttribute("download", `SIG_Mapdroid-${tagName}.apk`);
    }
  });

  // Tampilkan changelog jika tersedia
  if (release.body && document.getElementById("release-notes")) {
    setInner("release-notes", release.body.slice(0, 300) + (release.body.length > 300 ? "..." : ""));
  }
}

function handleNoReleaseYet() {
  console.log("CrootJS: Rilis pertama sedang dipersiapkan atau menunggu push ke GitHub.");
  setInner("badge-version", "v1.0.0 (Auto)");
  setInner("badge-size", "APK Release");
  setInner("release-tag-display", "v1.0.0");
  setInner("release-date-display", "Siap di GitHub");
  setInner("release-size-display", "~15 MB");
  setInner("release-status-text", "Siap Rilis Otomatis via Workflow Push");

  const downloadButtons = [
    document.getElementById("btn-download-hero"),
    document.getElementById("btn-download-card"),
    document.getElementById("btn-download-nav")
  ];

  downloadButtons.forEach(btn => {
    if (btn) {
      btn.href = GITHUB_FALLBACK_URL;
    }
  });
}

// 2. Inisialisasi Peta Web MapLibre GL JS
let map;
const campusCenter = [107.5758, -6.8732]; // [Lng, Lat] ULBI Bandung

const basemaps = {
  osm: {
    version: 8,
    sources: {
      'osm-tiles': {
        type: 'raster',
        tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
        tileSize: 256,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      }
    },
    layers: [
      {
        id: 'osm-layer',
        type: 'raster',
        source: 'osm-tiles',
        minzoom: 0,
        maxzoom: 19
      }
    ]
  },
  voyager: {
    version: 8,
    sources: {
      'carto-voyager': {
        type: 'raster',
        tiles: [
          'https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png',
          'https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png'
        ],
        tileSize: 256,
        attribution: '&copy; CARTO &copy; OpenStreetMap contributors'
      }
    },
    layers: [
      {
        id: 'voyager-layer',
        type: 'raster',
        source: 'carto-voyager'
      }
    ]
  },
  dark: {
    version: 8,
    sources: {
      'carto-dark': {
        type: 'raster',
        tiles: [
          'https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png',
          'https://b.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png'
        ],
        tileSize: 256,
        attribution: '&copy; CARTO &copy; OpenStreetMap contributors'
      }
    },
    layers: [
      {
        id: 'dark-layer',
        type: 'raster',
        source: 'carto-dark'
      }
    ]
  }
};

function initMapLibre() {
  const mapContainer = document.getElementById("map");
  if (!mapContainer) return;

  map = new maplibregl.Map({
    container: 'map',
    style: basemaps.osm,
    center: campusCenter,
    zoom: 16.5,
    pitch: 35,
    bearing: -10
  });

  // Navigasi & Kontrol Zoom
  map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), 'top-right');
  map.addControl(new maplibregl.FullscreenControl(), 'top-right');

  // Kontrol Lokasi GPS Pengguna di Browser
  map.addControl(new maplibregl.GeolocateControl({
    positionOptions: { enableHighAccuracy: true },
    trackUserLocation: true
  }), 'top-right');

  map.on('load', () => {
    loadGeoJsonData();
  });
}

function loadGeoJsonData() {
  fetch('data/kampus_ulbi.geojson')
    .then(res => res.json())
    .then(geojson => {
      // Tambahkan Source
      map.addSource('kampus-source', {
        type: 'geojson',
        data: geojson
      });

      // 1. Layer Poligon Gedung
      map.addLayer({
        id: 'kampus-buildings-fill',
        type: 'fill',
        source: 'kampus-source',
        filter: ['==', '$type', 'Polygon'],
        paint: {
          'fill-color': ['coalesce', ['get', 'fill'], '#0284c7'],
          'fill-opacity': 0.7
        }
      });

      // 2. Layer Garis Batas Gedung
      map.addLayer({
        id: 'kampus-buildings-line',
        type: 'line',
        source: 'kampus-source',
        filter: ['==', '$type', 'Polygon'],
        paint: {
          'line-color': ['coalesce', ['get', 'stroke'], '#0f172a'],
          'line-width': 2
        }
      });

      // 3. Layer Garis Jalan
      map.addLayer({
        id: 'kampus-roads-line',
        type: 'line',
        source: 'kampus-source',
        filter: ['==', '$type', 'LineString'],
        paint: {
          'line-color': ['coalesce', ['get', 'stroke'], '#3b82f6'],
          'line-width': 4
        }
      });

      // 4. Layer Titik POI
      map.addLayer({
        id: 'kampus-points-circle',
        type: 'circle',
        source: 'kampus-source',
        filter: ['==', '$type', 'Point'],
        paint: {
          'circle-radius': 9,
          'circle-color': ['coalesce', ['get', 'marker-color'], '#e11d48'],
          'circle-stroke-color': '#ffffff',
          'circle-stroke-width': 2
        }
      });

      setupMapInteractivity();
    })
    .catch(err => {
      console.error("Gagal memuat GeoJSON kampus:", err);
    });
}

function setupMapInteractivity() {
  const interactiveLayers = ['kampus-buildings-fill', 'kampus-points-circle', 'kampus-roads-line'];

  interactiveLayers.forEach(layerId => {
    map.on('mouseenter', layerId, () => {
      map.getCanvas().style.cursor = 'pointer';
    });
    map.on('mouseleave', layerId, () => {
      map.getCanvas().style.cursor = '';
    });
  });

  // Popup saat diklik
  map.on('click', (e) => {
    const features = map.queryRenderedFeatures(e.point, { layers: interactiveLayers });

    if (!features.length) return;

    const feature = features[0];
    const props = feature.properties || {};

    const name = props.nama || props.Gedung || props['Gedung '] || props.Sekre || props['gerbang '] || props['Jalan '] || 'Objek Spasial';
    const kategori = props.kategori || (feature.geometry.type === 'Polygon' ? 'Gedung Kampus' : (feature.geometry.type === 'LineString' ? 'Jalur / Jalan' : 'Titik Fasilitas'));
    const keterangan = props.keterangan || props.kelas || '-';

    new maplibregl.Popup()
      .setLngLat(e.lngLat)
      .setHTML(`
        <div class="text-left">
          <span class="inline-block px-2 py-0.5 text-xs font-semibold rounded bg-sky-100 text-sky-800 mb-1">${kategori}</span>
          <h4 class="font-bold text-base text-slate-900">${name}</h4>
          <p class="text-xs text-slate-500 mt-1"><strong>Keterangan:</strong> ${keterangan}</p>
          <p class="text-xs text-slate-400 mt-1">Koordinat: ${e.lngLat.lat.toFixed(5)}, ${e.lngLat.lng.toFixed(5)}</p>
        </div>
      `)
      .addTo(map);
  });
}

// Handler Basemap Switcher di Web
window.setWebBasemap = function(type) {
  if (!map || !basemaps[type]) return;
  map.setStyle(basemaps[type]);
  map.once('style.load', () => {
    loadGeoJsonData();
  });
};

// Handler Recenter Peta
window.recenterWebMap = function() {
  if (!map) return;
  map.flyTo({
    center: campusCenter,
    zoom: 16.5,
    pitch: 35,
    bearing: -10,
    essential: true
  });
};

// Toggle Layer
window.toggleLayer = function(layerId, checkbox) {
  if (!map || !map.getLayer(layerId)) return;
  map.setLayoutProperty(layerId, 'visibility', checkbox.checked ? 'visible' : 'none');
  if (layerId === 'kampus-buildings-fill') {
    map.setLayoutProperty('kampus-buildings-line', 'visibility', checkbox.checked ? 'visible' : 'none');
  }
};

// Eksekusi Inisialisasi saat Halaman Selesai Dimuat
document.addEventListener('DOMContentLoaded', () => {
  initReleaseTracker();
  initMapLibre();
});
