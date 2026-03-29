package com.example.permguide.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.permguide.R
import com.example.permguide.data.DataRepository
import com.example.permguide.model.Attraction
import com.example.permguide.utils.NotificationHelper
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider

class MapFragment : Fragment(), UserLocationObjectListener {
    private var _mapView: MapView? = null


    // 1. Меняем lateinit на обычный nullable, чтобы безопасно проверять его
    private var userLocationLayer: UserLocationLayer? = null
    private var attractionsList: List<Attraction> = listOf()
    private var selectedPlacemark: com.yandex.mapkit.map.PlacemarkMapObject? = null
    private val notifiedIds = mutableSetOf<Int>()
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var isPlaying = false
    private var handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val placemarkTapListener =
        com.yandex.mapkit.map.MapObjectTapListener { mapObject, _ ->

            val attraction = mapObject.userData as? Attraction ?: return@MapObjectTapListener true

            val lat = attraction.latitudeAttraction ?: return@MapObjectTapListener true
            val lon = attraction.longitudeAttraction ?: return@MapObjectTapListener true

            val point = Point(lat, lon)

            val placemark = mapObject as com.yandex.mapkit.map.PlacemarkMapObject

// 1. Проверяем старую метку перед тем, как вернуть ей обычный вид
            selectedPlacemark?.let {
                if (it.isValid) {
                    it.setIcon(
                        ImageProvider.fromBitmap(
                            bitmapFromVectorDrawable(R.drawable.map_pin)!!
                        )
                    )
                }
            }

// 2. Обновляем текущую выбранную метку
            selectedPlacemark = placemark

// 3. Проверяем новую метку перед покраской в зеленый
            if (placemark.isValid) {
                placemark.setIcon(
                    ImageProvider.fromBitmap(
                        bitmapFromVectorDrawable(R.drawable.map_pin_selected)!!
                    )
                )
            }
            //selectedPlacemark = placemark

            showPreview(attraction)

            _mapView?.map?.move(
                CameraPosition(point, 15f, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )

            true
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Проверка разрешений
        checkPermissions()

        val view = inflater.inflate(R.layout.fragment_map, container, false)
        _mapView = view.findViewById(R.id.mapview)

        val mapKit = MapKitFactory.getInstance()

        // 2. Исправляем логику создания слоя: создаем только ОДИН раз
        userLocationLayer = mapKit.createUserLocationLayer(_mapView!!.mapWindow).apply {
            isVisible = true
            isHeadingEnabled = true
            setObjectListener(this@MapFragment)
        }

        userLocationLayer?.let { layer ->
            layer.isVisible = true
            layer.isHeadingEnabled = true
            // 3. Устанавливаем слушатель ЗДЕСЬ, чтобы иконки подхватились
            layer.setObjectListener(this)

        }
        val styleJson = """
        [
          {
            "types": ["point"],
            "tags": { "all": ["poi"] },
            "stylers": { "visibility": "off" }
          },
          {
            "types": ["point"],
            "tags": { "all": ["transit"] },
            "stylers": { "visibility": "off" }
          }
        ]

            """.trimIndent()

        _mapView?.map?.setMapStyle(styleJson)
        // Центрирование на Перми при запуске
        _mapView?.map?.move(
            CameraPosition(Point(58.01045, 56.22944), 11.0f, 0.0f, 0.0f)
        )


        // Кнопка "Мое местоположение"
        view.findViewById<View>(R.id.btn_my_location).setOnClickListener {
            // Пытаемся взять позицию. Если null - значит GPS еще не поймал сигнал
            val location = userLocationLayer?.cameraPosition()
            if (location != null) {
                _mapView?.map?.move(
                    CameraPosition(location.target, 16f, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 1f),
                    null
                )
            } else {
                android.widget.Toast.makeText(context, "Поиск спутников...", android.widget.Toast.LENGTH_SHORT).show()
            }
        }



        // Зум
        view.findViewById<View>(R.id.zoom_in).setOnClickListener { changeZoom(1.0f) }
        view.findViewById<View>(R.id.zoom_out).setOnClickListener { changeZoom(-1.0f) }
        fetchAttractions()
        return view
    }
    private fun fetchAttractions() {

        val repo = DataRepository(requireContext())

        repo.getAttractions { fullList ->

            val unique = fullList
                .filter { it.idAttraction != 13 }
                .distinctBy { it.idAttraction }

            attractionsList = unique
            showAttractionsOnMap(unique)
        }
    }
    private fun showAttractionsOnMap(attractions: List<Attraction>) {

        val mapObjects = _mapView?.map?.mapObjects ?: return

        val bitmap = bitmapFromVectorDrawable(R.drawable.map_pin) ?: return
        val icon = ImageProvider.fromBitmap(bitmap)

        attractions.forEach { attraction ->

            val lat = attraction.latitudeAttraction
            val lon = attraction.longitudeAttraction

            if (lat != null && lon != null) {

                val point = Point(lat, lon)

                val placemark = mapObjects.addPlacemark(point, icon)
                placemark.userData = attraction
                placemark.addTapListener(placemarkTapListener)

            }
        }
    }

    // Смена иконок
    override fun onObjectAdded(userLocationView: UserLocationView) {
        updateUserIcon(userLocationView)
    }

    // Обязательно оставь эти методы, даже если они пустые
    override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {
        updateUserIcon(userLocationView)

        val location = userLocationLayer?.cameraPosition()
        if (location != null) {
            val lat = location.target.latitude
            val lon = location.target.longitude

            checkNearbyAttractions(lat, lon)
        }
    }
    override fun onObjectRemoved(p0: UserLocationView) {}
    private fun updateUserIcon(view: UserLocationView) {
        val context = context ?: return

        // Превращаем наш вектор в битмап
        val bitmap = bitmapFromVectorDrawable(R.drawable.user_pin)

        if (bitmap != null) {
            val imageProvider = ImageProvider.fromBitmap(bitmap)
            view.pin.setIcon(imageProvider)
            view.arrow.setIcon(imageProvider)
        }

        view.accuracyCircle.fillColor = android.graphics.Color.TRANSPARENT
    }
    private fun changeZoom(value: Float) {
        val map = _mapView?.map ?: return
        val pos = map.cameraPosition
        map.move(CameraPosition(pos.target, pos.zoom + value, pos.azimuth, pos.tilt),
            Animation(Animation.Type.SMOOTH, 0.2f), null)
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        val needsPermission = permissions.any {
            androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), it) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needsPermission) {
            requestPermissions(permissions, 100)
        }
    }
    private fun showPreview(attraction: Attraction) {

        val container = view?.findViewById<FrameLayout>(R.id.placePreviewContainer) ?: return

        container.removeAllViews()

        val preview = layoutInflater.inflate(R.layout.map_place_preview, container, false)

        val image = preview.findViewById<ImageView>(R.id.previewImage)
        val title = preview.findViewById<TextView>(R.id.previewTitle)
        val audio = preview.findViewById<ImageButton>(R.id.btnAudio)
        val close = preview.findViewById<ImageButton>(R.id.btnClose)
        val seekBar = preview.findViewById<SeekBar>(R.id.audioProgress)
        val currentTime = preview.findViewById<TextView>(R.id.textCurrentTime)
        val durationTime = preview.findViewById<TextView>(R.id.textDuration)

        title.text = attraction.nameAttraction

        // загрузка фото через Glide
        Glide.with(this)
            .load(attraction.photo)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .into(image)


        val audioUrl = attraction.audio
        audio.setOnClickListener {

            if (audioUrl.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Нет аудио", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isPlaying) {
                mediaPlayer?.pause()
                isPlaying = false
                audio.setImageResource(R.drawable.ic_play)
                return@setOnClickListener
            }

            // если уже есть плеер → продолжаем
            mediaPlayer?.let {
                it.start()
                isPlaying = true
                audio.setImageResource(R.drawable.ic_pause)
                return@setOnClickListener
            }

            // новый плеер
            val player = android.media.MediaPlayer()
            mediaPlayer = player

            player.apply {
                setDataSource(audioUrl)
                prepareAsync()

                setOnPreparedListener {
                    start()
                    this@MapFragment.isPlaying = true
                    audio.setImageResource(R.drawable.ic_pause)

                    updateProgress(seekBar, currentTime, durationTime)
                }

                setOnCompletionListener {
                    this@MapFragment.isPlaying = false
                    audio.setImageResource(R.drawable.ic_play)
                    seekBar.progress = 0
                    currentTime.text = "00:00"

                    release()
                    mediaPlayer = null
                }

                setOnErrorListener { _, _, _ ->
                    Toast.makeText(requireContext(), "Ошибка воспроизведения", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        preview.setOnClickListener {

            val fragment = AttractionDetailFragment()

            val bundle = Bundle().apply {
                putInt("id", attraction.idAttraction)
                putString("name", attraction.nameAttraction)
                putString("description", attraction.descriptionAttraction)
            }

            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
        }
        close.setOnClickListener {

            container.visibility = View.GONE

            // вернуть метку обратно
// 1. Проверяем старую метку перед тем, как вернуть ей обычный вид
            selectedPlacemark?.let {
                if (it.isValid) {
                    it.setIcon(
                        ImageProvider.fromBitmap(
                            bitmapFromVectorDrawable(R.drawable.map_pin)!!
                        )
                    )
                }
            }


            selectedPlacemark = null
        }

        container.addView(preview)
        container.visibility = View.VISIBLE
    }
    private fun updateProgress(
        seekBar: SeekBar,
        currentTime: TextView,
        durationTime: TextView
    ) {
        mediaPlayer?.let { player ->

            seekBar.max = player.duration
            durationTime.text = formatTime(player.duration)

            handler.post(object : Runnable {
                override fun run() {
                    if (player.isPlaying) {
                        seekBar.progress = player.currentPosition
                        currentTime.text = formatTime(player.currentPosition)
                        handler.postDelayed(this, 500)
                    }
                }
            })
        }
    }
    private fun formatTime(ms: Int): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    private fun bitmapFromVectorDrawable(drawableId: Int): android.graphics.Bitmap? {
        val drawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), drawableId) ?: return null
        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        _mapView?.onStart()
        userLocationLayer?.isVisible = true
    }

    override fun onStop() {
        _mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
// Важно: сначала обнуляем ссылки на объекты MapKit
        userLocationLayer?.setObjectListener(null)
        userLocationLayer = null
        selectedPlacemark = null
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null

        // Затем обнуляем сам MapView
        _mapView = null

        super.onDestroyView()
    }
    private fun checkNearbyAttractions(userLat: Double, userLon: Double) {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        val notificationsEnabled = prefs.getBoolean("notifications", false)
        val radius = prefs.getInt("radius", 200)

        if (!notificationsEnabled) return

        val notificationHelper = NotificationHelper(requireContext())

        for (attraction in attractionsList) {

            val lat = attraction.latitudeAttraction ?: continue
            val lon = attraction.longitudeAttraction ?: continue

            val distance = distanceInMeters(
                userLat, userLon,
                lat, lon
            )

            if (distance <= radius && !notifiedIds.contains(attraction.idAttraction)) {
                notifiedIds.add(attraction.idAttraction)

                notificationHelper.showNotification(
                    attraction.nameAttraction,
                    "Вы рядом! (${distance.toInt()} м)"
                )
            }
        }
    }
    fun distanceInMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(
            lat1, lon1,
            lat2, lon2,
            result
        )
        return result[0]
    }
}