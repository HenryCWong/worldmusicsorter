package kingdomofsheep.worldmusicsorter;

/**
 * Created by Overlord on 12/29/2015.
 * Query's the user's device for audio files
 */
public class Song
{
    private long id;
    private String title;
    private String artist;

    public Song(long songID, String songTitle, String songArtist)
    {
        id = songID;
        title=songTitle;
        artist=songArtist;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
}
