package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerYear;
import com.danga.squeezer.service.SqueezerServerState;

oneway interface IServiceYearListCallback {
  void onYearsReceived(int count, int pos, in List<SqueezerYear> albums);
  void onServerStateChanged(in SqueezerServerState oldState, in SqueezerServerState newState);
}

