package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.service.SqueezerServerState;

oneway interface IServiceArtistListCallback {
  void onArtistsReceived(int count, int start, in List<SqueezerArtist> artists);
  void onServerStateChanged(in SqueezerServerState oldState, in SqueezerServerState newState);
}

