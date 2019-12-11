package temple.edu;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


public class BookDetailsFragment extends Fragment {

    private String downUrl = "https://kamorris.com/lab/audlib/download.php?id=";
    private TextView tvBook;
    private Button btPlay;
    private ImageView image;
    private Book book;
    private Button btDownload;
    private ProgressDialog dialog;

    public static BookDetailsFragment newInstance(Book book) {
        BookDetailsFragment bookDetailsFragment = new BookDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable("book",book);
        bookDetailsFragment.setArguments(args);
        return bookDetailsFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_detail, container, false);
        tvBook = view.findViewById(R.id.tv_title);
        image = view.findViewById(R.id.image);
        btPlay = view.findViewById(R.id.btPlay);
        btDownload = view.findViewById(R.id.bt_download);

        if (getArguments()!=null) {
             book = (Book) getArguments().getSerializable("book");
            if (book != null) {
                displayBook(book);
            }
        }
        btPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener!=null) {
                    listener.play(book);
                }
            }
        });
        btDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bookIsDownload()) {
                    deleteLocal();
                } else{
                    dialog = new ProgressDialog(getActivity());
                dialog.setTitle("Download");
                dialog.show();
                new DownTask(getActivity()).execute();
                }
            }
        });
        showStatus();
        return view;
    }

    private void deleteLocal() {
        String filePath =getActivity(). getFilesDir()
                + File.separator + book.id  + ".mp3";
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
            showStatus();
        }
    }

    private boolean bookIsDownload(){

        String filePath =getActivity(). getFilesDir()
                + File.separator + book.id  + ".mp3";
        File file = new File(filePath);
        if(file.exists()){
            return  true;
        }
        return false;
    }
    private void showStatus(){
        String filePath = getActivity().getFilesDir()
                + File.separator + book.id + ".mp3";
        File file = new File(filePath);
        if(file.exists()){
            btDownload.setText("Delete");
        }else{
            btDownload.setText("Download");
        }
    }

    public void displayBook(Book book) {
        this.book = book;
        tvBook.setText(book.title);
        Picasso.get().load(book.cover_url).fit().into(image);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof BookDetailsFragment.PlayListener) {
            listener = (BookDetailsFragment.PlayListener) context;
        }
    }
    PlayListener listener;
    public interface PlayListener {
        void play(Book book);
    }

    private  class DownTask extends AsyncTask<Void, Integer, Boolean> {
        Context context;

        public DownTask(Context context) {
            this.context = context;
        }
        @Override
        protected Boolean doInBackground(Void... voids) {

            try {
                URL url = new URL(downUrl+book.id);
                URLConnection urlConnection = url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                int contentLength = urlConnection.getContentLength();
                String filePath = context.getFilesDir()
                        + File.separator + book.id + ".mp3";
                Log.e("value",filePath+"");
                int downloadSize = 0;
                byte[] bytes = new byte[1024];
                int length;
                OutputStream outputStream = new FileOutputStream(filePath);
                while ((length = inputStream.read(bytes)) != -1){
                    outputStream.write(bytes,0,length);
                    downloadSize += length;
                    publishProgress(downloadSize * 100/contentLength);
                }
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("value",e.getLocalizedMessage()+"");
                return false;
            }

            return true;
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Log.e("value",values[0]+"");
            dialog.setMessage(values[0]+"%");
        }


        @Override
        protected void onPostExecute(Boolean download) {
            dialog.dismiss();
            if(download) {
                showStatus();
                Toast.makeText(context, "downloaded", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Downloaded error", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
