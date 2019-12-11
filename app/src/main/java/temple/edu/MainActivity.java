package temple.edu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import edu.temple.audiobookplayer.AudiobookService;

public class MainActivity extends AppCompatActivity implements BookDetailsFragment.PlayListener {

    private ArrayList<Book> books = new ArrayList<>();
    private BookListFragment listFragment;
    private BookDetailsFragment detailFragment;
    private EditText etSearch;
    private Button btSearch;
    private ViewPagerFragment viewpagerFragment;
    AudiobookService.MediaControlBinder binder;
    private SeekBar seekBar;
    private Button stopButton;
    private Button pauseButton;
    private Intent serviceIntent;
    private boolean isPlay = false;
    private TextView tvPlaying;
    private int duration;
    private boolean mServiceConnected;
    private int bookId = -1;
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
         sharedPreferences =  getSharedPreferences("book",MODE_PRIVATE);
        etSearch = findViewById(R.id.et_content);
        btSearch = findViewById(R.id.bt_search);
        stopButton = findViewById(R.id.stop_button);
        pauseButton = findViewById(R.id.pause_button);
        tvPlaying = findViewById(R.id.tv_playing);
        seekBar = findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (bookId!=-1) {
                    if (fromUser && mServiceConnected) {
                        if (bookIsDownload()){
                            String filePath = getFilesDir()
                                    + File.separator +bookId + ".mp3";
                            File file = new File(filePath);
                            binder.play(file,progress);
                        }else{
                            binder.play(bookId, progress);
                        }

                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        btSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = etSearch.getText().toString();
                searchBooks(key);
            }
        });
        WindowManager wm = getWindowManager();
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        if (height > width) {
            viewpagerFragment = ViewPagerFragment.newInstance();
            fragmentTransaction.replace(R.id.fm_content, viewpagerFragment);
        } else {
            listFragment = BookListFragment.newInstance();
            detailFragment = new BookDetailsFragment();
            fragmentTransaction.replace(R.id.fl_list, listFragment);
            fragmentTransaction.replace(R.id.fl_content, detailFragment);
            listFragment.addSelectListener(new BookListFragment.OnItemSelectedListener() {
                @Override
                public void onItemSelected(Book book) {
                    detailFragment.displayBook(book);
                }
            });

        }
        fragmentTransaction.commit();
        serviceIntent= new Intent(this, AudiobookService.class);
        bindService(serviceIntent, ServiceConnection, Context.BIND_AUTO_CREATE);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binder.stop();
                tvPlaying.setText("");
                sharedPreferences.edit().putInt(bookId+"",0).commit();
                seekBar.setProgress(0);
                isPlay = false;
                bookId = -1;

                stopService(serviceIntent);
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPreferences.edit().putInt(bookId+"",seekBar.getProgress()).commit();
                binder.pause();

            }
        });
       String local =  sharedPreferences.getString("books","");
       if (TextUtils.isEmpty(local)) {
           searchBooks("");
       }else{
           Message msg = Message.obtain();
           msg.obj = local;
           handler.sendMessage(msg);

       }

    }
    private boolean bookIsDownload(){
        if (bookId==-1){
            return  false;
        }
        String filePath = getFilesDir()
                + File.separator +bookId + ".mp3";
        File file = new File(filePath);
        if(file.exists()){
           return  true;
        }
        return false;
    }
    @Override
    public void play(Book book) {
        bookId =book. id;
        sharedPreferences.edit().putInt("playing",bookId).commit();
        startService(serviceIntent);
        duration= book.duration;
        seekBar.setMax(book.duration);
        String title = book.title;
        tvPlaying.setText("Playing:"+title);
        isPlay = true;
        if (bookIsDownload()){
            String filePath = getFilesDir()
                    + File.separator +bookId + ".mp3";
            File file = new File(filePath);
            int startPosition = sharedPreferences.getInt(bookId+"",0);
            binder.play(file, startPosition);
        }else{
            binder.play(bookId);
        }
    }


    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            try{

                sharedPreferences.edit().putString("books",(String)msg.obj).commit();
                JSONArray  bookArray = new JSONArray((String)msg.obj);

                if(viewpagerFragment!=null){
                    for(int i = 0 ; i < bookArray.length(); i++){
                        JSONObject jsonObject = bookArray.optJSONObject(i);
                        Book book = new Book();
                        book.id = jsonObject.optInt("book_id");
                        book.title = jsonObject.optString("title");
                        book.author= jsonObject.optString("author");
                        book.published = jsonObject.optInt("published");
                        book.cover_url= jsonObject.optString("cover_url");
                        book.duration= jsonObject.optInt("duration");
                        books.add(book);
                    }
                    viewpagerFragment.setBooks(books);

                }else if (listFragment!=null){
                    listFragment.setBooks(bookArray);
                    detailFragment.displayBook(listFragment.getBook());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            int bookid =  sharedPreferences.getInt("playing",-1);
            for (int i = 0; i < books.size(); i++) {
                if (books.get(i).id==bookid){
                    Log.e("id",books.get(i).id+"");
                    duration= books.get(i).duration;
                    seekBar.setMax(books.get(i).duration);
                    String title = books.get(i).title;
                    tvPlaying.setText("Playing:"+title);
                }
            }
        }
    };
    private void searchBooks(final  String key) {
        new Thread(){
            public void run(){
                try{
                    String urlStr = "https://kamorris.com/lab/audlib/booksearch.php?search=" + key;
                    URL url = new URL(urlStr);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                    StringBuilder builder = new StringBuilder();
                    String tmpString;

                    while((tmpString = reader.readLine()) != null){
                        builder.append(tmpString);
                    }
                    Message msg = Message.obtain();
                    msg.obj = builder.toString();
                    handler.sendMessage(msg);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    android.content.ServiceConnection ServiceConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceConnected = true;
            binder = (AudiobookService.MediaControlBinder) service;
            binder.setProgressHandler(progressHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceConnected = false;
            binder = null;
        }
    };

    Handler progressHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.obj != null) {
                AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) msg.obj;
                Log.e("tag",bookProgress.getProgress()+"");
                if (bookProgress.getProgress() <duration) {
                    seekBar.setProgress(bookProgress.getProgress());
                } else if (bookProgress.getProgress() == duration) {
                    binder.stop();
                    tvPlaying.setText("");
                    seekBar.setProgress(0);
                    isPlay = false;
                    bookId = -1;
                }
            }
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(ServiceConnection);
        if (bookIsDownload()){
          sharedPreferences.edit().putInt(bookId+"",seekBar.getProgress()).commit();
          sharedPreferences.edit().putInt("playing",bookId).commit();
        }
    }


}
