package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.service.SqueezerServerState;

oneway interface IServiceAlbumListCallback {
  void onAlbumsReceived(int count, int start, in List<SqueezerAlbum> albums);
  void onServerStateChanged(in SqueezerServerState oldState, in SqueezerServerState newState);
}

