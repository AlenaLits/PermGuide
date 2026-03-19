package com.example.permguide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.permguide.R
import com.example.permguide.data.DataRepository
import com.example.permguide.model.Attraction
import com.example.permguide.network.RetrofitClient
import com.example.permguide.utils.AudioCacheManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class HomeFragment : Fragment() {
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var isPlaying = false
    private var handler = android.os.Handler(android.os.Looper.getMainLooper())
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val cityDesc = view.findViewById<TextView>(R.id.tvHomeCityDesc)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerCityPhotos)
        val dots = view.findViewById<WormDotsIndicator>(R.id.dotsIndicator)
        val btnPlayAudio = view.findViewById<View>(R.id.btnPlayAudioHome)
        val playIcon = view.findViewById<ImageView>(R.id.playIconId)
        val seekBar = view.findViewById<SeekBar>(R.id.audioProgress)
        val currentTime = view.findViewById<TextView>(R.id.textCurrentTime)
        val durationTime = view.findViewById<TextView>(R.id.textDuration)
        val repo = DataRepository(requireContext().applicationContext)

        // Запрос к API для получения данных о городе
        repo.getAttractions { allPlaces ->
            if (!isAdded) return@getAttractions

            // Берём объект с idAttraction = 13
            val cityData = allPlaces.find { it.idAttraction == 13 }

            // Описание города
            cityDesc.text = cityData?.descriptionAttraction

            // Берём все фото для этой достопримечательности
            val cityPhotos = allPlaces
                .filter { it.idAttraction == 13 }
                .mapNotNull { it.photo }

            if (cityPhotos.isNotEmpty()) {
                viewPager.adapter = PhotoPagerAdapter(cityPhotos)
                dots.attachTo(viewPager)
            }

            val audioUrl = cityData?.audio
            // Кнопка аудио
            btnPlayAudio.setOnClickListener {

                if (audioUrl.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Нет аудио", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                    playIcon.setImageResource(R.drawable.ic_play)
                    return@setOnClickListener
                }

                // если уже есть плеер → продолжаем
                mediaPlayer?.let {
                    it.start()
                    isPlaying = true
                    playIcon.setImageResource(R.drawable.ic_pause)
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
                        this@HomeFragment.isPlaying = true
                        playIcon.setImageResource(R.drawable.ic_pause)

                        updateProgress(seekBar, currentTime, durationTime)
                    }

                    setOnCompletionListener {
                        this@HomeFragment.isPlaying = false
                        playIcon.setImageResource(R.drawable.ic_play)
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
        }

        return view
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
    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroyView()
    }
}