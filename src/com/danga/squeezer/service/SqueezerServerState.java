package com.danga.squeezer.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class SqueezerServerState {
	private final AtomicBoolean mRescan = new AtomicBoolean(false);
	private final AtomicReference<Integer> mLastScan = new AtomicReference<Integer>();
	private final AtomicReference<String> mVersion = new AtomicReference<String>();
	private final AtomicReference<String> mUuid = new AtomicReference<String>();
	private final AtomicReference<Integer> mTotalAlbums = new AtomicReference<Integer>();
	private final AtomicReference<Integer> mTotalArtists = new AtomicReference<Integer>();
	private final AtomicReference<Integer> mTotalGenres = new AtomicReference<Integer>();
	private final AtomicReference<Integer> mTotalSongs = new AtomicReference<Integer>();
	private final AtomicReference<Integer> mPlayerCount = new AtomicReference<Integer>();
	private final AtomicReference<Integer> mSeenPlayerCount = new AtomicReference<Integer>();
	private final AtomicReference<Integer> mOtherPlayerCount = new AtomicReference<Integer>();

	public Boolean getRescan() {
		return mRescan.get();
	}
	
	public SqueezerServerState setRescan(Boolean rescan) {
		mRescan.set(rescan);
		return this;
	}
	
	public Integer getLastScan() {
		return mLastScan.get();
	}
	
	public SqueezerServerState setLastScan(Integer lastscan) {
		mLastScan.set(lastscan);
		return this;
	}

	public String getVersion() {
		return mVersion.get();
	}
	
	public SqueezerServerState setVersion(String version) {
		mVersion.set(version);
		return this;
	}

	public String getUuid() {
		return mUuid.get();
	}
	
	public SqueezerServerState setUuid(String uuid) {
		mUuid.set(uuid);
		return this;
	}
	
	public Integer getTotalAlbums() {
		return mTotalAlbums.get();
	}
	
	public SqueezerServerState setTotalAlbums(Integer total) {
		mTotalAlbums.set(total);
		return this;
	}

	public Integer getTotalArtists() {
		return mTotalArtists.get();
	}
	
	public SqueezerServerState setTotalArtists(Integer total) {
		mTotalArtists.set(total);
		return this;
	}

	public Integer getTotalGenres() {
		return mTotalGenres.get();
	}
	
	public SqueezerServerState setTotalGenres(Integer total) {
		mTotalGenres.set(total);
		return this;
	}

	public Integer getTotalSongs() {
		return mTotalSongs.get();
	}
	
	public SqueezerServerState setTotalSongs(Integer total) {
		mTotalSongs.set(total);
		return this;
	}

	public Integer getPlayerCount() {
		return mPlayerCount.get();
	}
	
	public SqueezerServerState setPlayerCount(Integer total) {
		mPlayerCount.set(total);
		return this;
	}
	public Integer getSeenPlayerCount() {
		return mSeenPlayerCount.get();
	}
	
	public SqueezerServerState setSeenPlayerCount(Integer total) {
		mSeenPlayerCount.set(total);
		return this;
	}

	public Integer getOtherPlayerCount() {
		return mOtherPlayerCount.get();
	}
	
	public SqueezerServerState setOtherPlayerCount(Integer total) {
		mOtherPlayerCount.set(total);
		return this;
	}
	
}
