package com.example.rxplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class SongAdapter(
    private val context: Context,
    private val songList: ArrayList<Song>,
    private val songClickListener: OnSongClickListener
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    interface OnSongClickListener {
        fun onSongClick(position: Int)
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSongTitle: TextView = itemView.findViewById(R.id.tvSongTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ivAlbumArt: ImageView = itemView.findViewById(R.id.ivAlbumArt)

        init {
            itemView.setOnClickListener {
                songClickListener.onSongClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songList[position]
        holder.tvSongTitle.text = song.title
        holder.tvArtist.text = song.artist

        // Load album art using Glide
        Glide.with(context)
            .load(song.albumArt)
            .placeholder(R.drawable.ic_music_note)
            .into(holder.ivAlbumArt)
    }
}