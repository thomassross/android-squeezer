package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.service.SqueezerServerState;

oneway interface IServiceSongListCallback {
  void onSongsReceived(int count, int pos, in List<SqueezerSong> songs);
  void onServerStateChanged(in SqueezerServerState oldState, in SqueezerServerState newState);
  void onItemsFinished();
}

