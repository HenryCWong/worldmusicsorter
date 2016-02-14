package kingdomofsheep.worldmusicsorter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Overlord on 12/30/2015.
 * This is where the media playback happens
 */



public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {
    //Media Player
    private MediaPlayer player;
    //song List
    private ArrayList<Song> songs;
    //current position
    private int songPosn;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle=" ";
    private static final int NOTIFY_ID=1;
    private boolean shuffle=false;
    private Random rand;


    public void onCreate() {
        //create the service
        super.onCreate();
        //initialize position
        songPosn = 0;
        //create player
        player = new MediaPlayer();
        initMusicPlayer();
        rand = new Random();          //instantiate rnadom number generator
    }

    @Override
    public IBinder onBind(Intent intent) {    //amends the onBind method to return to this object
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {  //releases resources when the Service instance is unbound
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();
        //generates the notifications so the user knows what song is playing
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    public void initMusicPlayer() {
        //set player properties
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);   //when media player is prepared
        player.setOnCompletionListener(this); //when a song has completed the playback
        player.setOnErrorListener(this);      //whenever an error is thrown
    }

    public void setList(ArrayList<Song> theSongs) {    //passes the list of songs from the Activity
        songs = theSongs;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player.getCurrentPosition() > 0) {    // calls the playNext() method if the current track has reached it's end
            mp.reset();
            playNext();
        }

    }

    //stops the setForeground  when the instance is destroyed
    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();            //just resets the player
        return false;
    }

    public class MusicBinder extends Binder {          //help sycing with Activity and Service classes
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong() {                          //plays the actual track
        //play song
        player.reset();                             //resets the MediaPlayer class
        //get song
        Song playSong = songs.get(songPosn);
        //get title
        songTitle = playSong.getTitle();
        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);

        try {                                        //in case an exception may be thrown
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e) {
            Log.e("MUSIC SERVICE", "Sorry, there's been an error setting the data source", e);

            player.prepareAsync();  // calls the asynchornous method of the Mediaplayer class to prepare the playSong method
        }
    }

    public void setSong(int songIndex) {     //sets the current song
        songPosn=songIndex;
    }
    public int getPosn(){
        return player.getCurrentPosition();
    }
    public int getDur(){
        return player.getDuration();
    }
    public boolean isPng(){
        return player.isPlaying();
    }
    public void pausePlayer(){
        player.pause();
    }
    public void seek(int posn){
        player.seekTo(posn);
    }
    public void go(){
        player.start();
    }

    //skips to previous track
    public void playPrev(){
        songPosn--;
        if(songPosn<0)
            songPosn=songs.size()-1;
            playSong();
    }
    //skips to next track
    //if the shuffle flag is on, the player chooses a new song from a random list making sure it doesn't repeat the last one
    public void playNext() {
        if(shuffle) {
            int newSong = songPosn;
            while(newSong==songPosn) {
                newSong=rand.nextInt(songs.size());
            }
            songPosn=newSong;
        }
        else {
            songPosn++;
            if (songPosn >= songs.size()) songPosn = 0;
        }
            playSong();
    }

    //shuffle flag
    public void setShuffle(){
        if(shuffle)
            shuffle=false;
        else shuffle=true;
    }



}