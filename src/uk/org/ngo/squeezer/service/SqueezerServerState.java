
package uk.org.ngo.squeezer.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import uk.org.ngo.squeezer.framework.SqueezerItem;
import android.os.Parcel;

public class SqueezerServerState extends SqueezerItem {
    /*
     * TODO: Suspect that the use of these AtomicReferences is wrong. Should
     * either use Atomic[Type] instead, or follow the statistics update example
     * at http://www.javamex.com/tutorials/
     * synchronization_concurrency_7_atomic_reference.shtml for updates to this
     * object.
     */

    /** The server is currently scanning the music database. */
    private final AtomicBoolean mRescan = new AtomicBoolean(false);

    /** Timestamp when the last scan finished. 0 (not null) if unknown. */
    private final AtomicReference<Integer> mLastScan = new AtomicReference<Integer>(new Integer(0));

    /** Squeezebox Server version. */
    private final AtomicReference<String> mVersion = new AtomicReference<String>();
    private final AtomicReference<String> mUuid = new AtomicReference<String>();
    private final AtomicReference<Integer> mTotalAlbums = new AtomicReference<Integer>();
    private final AtomicReference<Integer> mTotalArtists = new AtomicReference<Integer>();
    private final AtomicReference<Integer> mTotalGenres = new AtomicReference<Integer>();
    private final AtomicReference<Integer> mTotalSongs = new AtomicReference<Integer>();
    private final AtomicReference<Integer> mTotalYears = new AtomicReference<Integer>();
    private final AtomicReference<Integer> mPlayerCount = new AtomicReference<Integer>();
    private final AtomicReference<Integer> mSeenPlayerCount = new AtomicReference<Integer>();
    private final AtomicReference<Integer> mOtherPlayerCount = new AtomicReference<Integer>();

    public SqueezerServerState() {

    }

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

    public Integer getTotalYears() {
        return mTotalYears.get();
    }

    public SqueezerServerState setTotalYears(Integer total) {
        mTotalYears.set(total);
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

    public static final Creator<SqueezerServerState> CREATOR = new Creator<SqueezerServerState>() {
        public SqueezerServerState[] newArray(int size) {
            return new SqueezerServerState[size];
        }

        public SqueezerServerState createFromParcel(Parcel source) {
            return new SqueezerServerState(source);
        }
    };

    private SqueezerServerState(Parcel source) {
        mRescan.set((Boolean) source.readValue(null));
        mLastScan.set(source.readInt());
        mVersion.set(source.readString());
        mUuid.set(source.readString());
        mTotalAlbums.set(source.readInt());
        mTotalArtists.set(source.readInt());
        mTotalGenres.set(source.readInt());
        mTotalSongs.set(source.readInt());
        mPlayerCount.set(source.readInt());
        mSeenPlayerCount.set(source.readInt());
        mOtherPlayerCount.set(source.readInt());
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mRescan.get());
        dest.writeInt(mLastScan.get());
        dest.writeString(mVersion.get());
        dest.writeString(mUuid.get());
        dest.writeInt(mTotalAlbums.get());
        dest.writeInt(mTotalArtists.get());
        dest.writeInt(mTotalGenres.get());
        dest.writeInt(mTotalSongs.get());
        dest.writeInt(mPlayerCount.get());
        dest.writeInt(mSeenPlayerCount.get());
        dest.writeInt(mOtherPlayerCount.get());
    }

    @Override
    public String toString() {
        return "id=" + getId() + ", uuid=" + mUuid.get();
    }

    @Override
    public String getName() {
        return mUuid.get();
    }

}
