package kingdomofsheep.worldmusicsorter;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import kingdomofsheep.worldmusicsorter.MusicService.MusicBinder;

/*
Controls the Music
The Music is played from the Service class
 */

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;     // represents the Service class
    private Intent playIntent;         //represents the Intent
    private boolean musicBound=false;  // keeps track of whether the Activity class is boudn to the Service class
    private MusicController controller;
    private boolean paused=false, playbackPaused=false;     //used to cope with uers returning to the app after leaving it

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       //retrieve list view
        songView = (ListView) findViewById(R.id.song_list);
        //instantiate list
        songList = new ArrayList<Song>();
        //get songs from device
        Collections.sort
                (songList, new Comparator<Song>()
        {
                    public int compare(Song a, Song b)
                    {
                    return a.getTitle().compareTo(b.getTitle());
                    }
        }
                );

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();
    }

    @Override                       // allows the Service Instance when the Activity instance starts
    protected void onStart() {
        super.onStart();
        if(playIntent==null){     // when the activity class starts, create the Intent object if it doens't already exist
            playIntent=new Intent(this,MusicService.class);
            bindService(playIntent,musicConnection, Context.BIND_AUTO_CREATE);   //bind it
            startService(playIntent);                                            //start it
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {   //checks binding status
            musicBound = false;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //switch statment for the end button
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                //shuffle
                musicSrv.setShuffle();                   //allows the user to toggle the shuffle switch
                break;
            case R.id.action_end:                        //stops the Service instance and exits the app
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    @Override
    protected void onPause() {     //flag for users to return to the app after leaving
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume() {      //ensures the the app or controller displays after the user exists and wants to access it again
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop() {      //hides it
        controller.hide();
        super.onStop();
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward(){
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }


    // additional test in order to avoid various exceptions that might occur when using the MediaPlayer and MediaController classess
    //Hint: Media Playback throws a lot of exceptions
    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public void start() {
        musicSrv.go();
    }



    public void getSongList()
    {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null,null,null,null);

        if(musicCursor!=null && musicCursor.moveToFirst())
        {
            //get columns
            int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);

            //add songs to list
            do
            {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    // sets the song position as the tag for each item in the list view when defined in the Adapter class
    //retrieves and passes it to the Service instance before calling the method to start playback
    //user song select
    public void songPicked(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }



    private void setController(){
        //set the controller up
        controller = new MusicController(this);
        //set previous and next button listeners
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        //set and show
        //works on media playback throughout the app with its anchor view referring to the list in the layout
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);


    }

    //play next
    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused) {
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    //play previous
    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }
}
