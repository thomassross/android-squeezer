package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerGenre;
import com.danga.squeezer.service.SqueezerServerState;

oneway interface IServiceGenreListCallback {
  void onGenresReceived(int count, int pos, in List<SqueezerGenre> albums);
  void onServerStateChanged(in SqueezerServerState oldState, in SqueezerServerState newState);
}

