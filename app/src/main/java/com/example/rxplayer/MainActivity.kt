package com.example.rxplayer

import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION_CODE = 100
    private lateinit var recyclerViewSongs: RecyclerView
    private lateinit var tvSongTitle: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var etSearch: EditText
    private lateinit var seekBar: SeekBar

    private lateinit var songList: ArrayList<Song>
    private lateinit var songAdapter: SongAdapter

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongIndex: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private var isSeekBarTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        recyclerViewSongs = findViewById(R.id.recyclerViewSongs)
        tvSongTitle = findViewById(R.id.tvSongTitle)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        etSearch = findViewById(R.id.etSearch)
        seekBar = findViewById(R.id.seekBar)

        if (checkPermission()) {
            loadMusic()
        }

        // Handle SeekBar progress change

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            mediaPlayer?.seekTo(progress)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        isSeekBarTracking = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        isSeekBarTracking = false
                    }
                })
        // Set up search functionality
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSongs(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                mediaPlayer?.start()
                btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }

        btnNext.setOnClickListener {
            playNextSong()
        }

        btnPrev.setOnClickListener {
            playPreviousSong()
        }
    }
    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_DENIED) {
                val permissions = arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
                requestPermissions(permissions, STORAGE_PERMISSION_CODE)
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusic()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // When the song is played or the title is updated
    private fun updateSongTitle(songTitle: String) {
        val tvSongTitle: TextView = findViewById(R.id.tvSongTitle)
        tvSongTitle.text = songTitle
        // Load the slide-in animation
        val slideInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)

        // Optionally, also load the fade-in animation if you want
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Apply the slide-in animation to the TextView
        tvSongTitle.startAnimation(slideInAnimation)

        // Apply the fade-in animation if you want it to fade
        tvSongTitle.startAnimation(fadeInAnimation)

    }


    // Permission methods...

    private fun loadMusic() {
        songList = ArrayList()
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        val cursor = contentResolver.query(uri, null, selection, null, sortOrder)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))

                    // Get album art
                    val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()

                    val song = Song(id, title, artist, data, duration, albumArtUri)
                    songList.add(song)

                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        // Set up RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(this, songList, object : SongAdapter.OnSongClickListener {
            override fun onSongClick(position: Int) {
                playSong(position)
            }
        })

        recyclerViewSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = songAdapter
        }
    }
    private fun filterSongs(query: String) {
        val filteredList = songList.filter { song ->
            song.title.contains(query, ignoreCase = true) || song.artist.contains(query, ignoreCase = true)
        }
        songAdapter = SongAdapter(this, ArrayList(filteredList), object : SongAdapter.OnSongClickListener {
            override fun onSongClick(position: Int) {
                playSong(position)
            }
        })
        recyclerViewSongs.adapter = songAdapter
    }

    private fun playSong(position: Int) {
        // Reset and release MediaPlayer if it's already playing
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }

        currentSongIndex = position
        val song = songList[position]
        val intent = Intent(this, NowPlayingActivity::class.java)
        intent.putExtra("SONG_TITLE", song.title)
        intent.putExtra("ARTIST_NAME", song.artist)
        intent.putExtra("ALBUM_ART", song.albumArt) // Optional: Pass URI or resource ID for album art
        startActivity(intent)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.data)
            prepare()
            start()
        }
        // Set the song title with animation
        updateSongTitle(song.title)

        // Update the play/pause button icon
        btnPlayPause.setImageResource(R.drawable.ic_pause)
        // Set up seek bar maximum and update UI
        seekBar.max = mediaPlayer!!.duration
        tvSongTitle.text = song.title
        btnPlayPause.setImageResource(R.drawable.ic_pause)

        // Update SeekBar progress
        updateSeekBar()

        // Update UI
        tvSongTitle.text = song.title
        btnPlayPause.setImageResource(R.drawable.ic_pause)

        // Handle completion
        mediaPlayer?.setOnCompletionListener {
            playNextSong()
        }
    }

    private fun playNextSong() {
        currentSongIndex = (currentSongIndex + 1) % songList.size
        playSong(currentSongIndex)
    }

    private fun playPreviousSong() {
        currentSongIndex = if (currentSongIndex - 1 < 0) songList.size - 1 else currentSongIndex - 1
        playSong(currentSongIndex)
    }
    // Update the SeekBar as the song progresses
    private fun updateSeekBar() {
        if (!isSeekBarTracking) {
            seekBar.progress = mediaPlayer?.currentPosition ?: 0
        }

        // Update every second
        handler.postDelayed({ updateSeekBar() }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}



