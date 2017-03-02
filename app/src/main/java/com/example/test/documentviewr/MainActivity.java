package com.example.test.documentviewr;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;




public class MainActivity extends AppCompatActivity {

    Button word,excel,pdf,tiff,openoff,mp4,png,jpeg,bmp,gpg,save,share,upload;
    WebView webView;
    Context context;
    Uri selectedFileUri;
    private int FILE_SIZE_LIMIT_IN_MB = 10;

    private String DOCUMENT_URL = "https://www.acm.org/sigs/publications/pubform.doc";
    private final int REQUEST_WRITE_STORAGE = 1231;
    private final int REQUEST_READ_FILE  = REQUEST_WRITE_STORAGE + 1;
    private boolean isPermissionAllowed = false;
    private final String SDCARD_PATH = "/sdcard/";

    final String imgUrl = "http://www.iconarchive.com/download/i6151/custom-icon-design/" +
            "pretty-office-4/JPG.ico";

    private String[] supportedFileTypes = {"pdf","mp3","mp4"};

    private static final String HTML_FORMAT = "<html><body style=\"text-align: center; background-color: black; vertical-align: center;\"><img src = \"%s\" /></body></html>";
    private int FILE_REQUEST_CODE = 345;
    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;


        webView = (WebView)findViewById(R.id.webview);
        share = (Button)findViewById(R.id.share);
        word = (Button)findViewById(R.id.word);
        png = (Button)findViewById(R.id.png);
        mp4 = (Button)findViewById(R.id.mp4);
        jpeg = (Button)findViewById(R.id.jpeg);
        save = (Button)findViewById(R.id.save);
        upload = (Button)findViewById(R.id.upload);


        webView.setWebViewClient(new AppWebViewClients());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);



        mp4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String frameVideo = "<html><body>Sample Video<br><iframe width=\"420\" height=\"315\" src=\"http://techslides.com/demos/sample-videos/small.mp4\" frameborder=\"0\" allowfullscreen></iframe></body></html>";
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return false;
                    }
                });
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webView.loadData(frameVideo, "text/html", "utf-8");
            }
        });

        png.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String html = String.format(HTML_FORMAT, imgUrl);
                webView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");

            }
        });

        word.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String docurl = "https://www.acm.org/sigs/publications/pubform.doc";
                webView.loadUrl("http://docs.google.com/gview?embedded=true&url="
                        + docurl);

            }
        });


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean hasPermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                } else {
                    new DownloadFileFromURL().execute(DOCUMENT_URL);
                }

            }
        });


        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                File fileDirectory = new File(SDCARD_PATH+getFileNameFromURL(DOCUMENT_URL));

                if(fileDirectory.exists()) {
                    intentShareFile.setType(getMimeType(getFileNameFromURL(DOCUMENT_URL)));
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+SDCARD_PATH+getFileNameFromURL(DOCUMENT_URL)));
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                            "Sharing File...");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                }

            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                if(intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, FILE_REQUEST_CODE);
                } else {
                    Toast.makeText(MainActivity.this,"No application supports this action",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        jpeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String urlString = "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";

                final String mimeType = "text/html";
                final String encoding = null;

                //image64 is the base64 string returned from the server


                String pageData = "<img src=\"data:image/jpeg;base64," + urlString + "\" />";



                webView.loadDataWithBaseURL("fake://not/needed", pageData, mimeType, encoding, "");

            }
        });

    }


    public class AppWebViewClients extends WebViewClient {



        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // TODO Auto-generated method stub
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // TODO Auto-generated method stub
            super.onPageFinished(view, url);

        }
    }



    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        String urlTODownload;
        String fileName;
        ProgressDialog progress;
        private String TAG = DownloadFileFromURL.class.getSimpleName();

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                urlTODownload = url.toString();
                fileName = getFileNameFromURL(urlTODownload);
                trustAllCertificates();
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output = new FileOutputStream(SDCARD_PATH+fileName);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress(""+(int)((total*100)/lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
                Log.i(TAG, "doInBackground: Download Success");

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissProgressDialog();
            String filePath = Environment.getExternalStorageDirectory().toString() + fileName;
        }


        void showProgressDialog(){
            if(progress == null) {
                progress = new ProgressDialog(MainActivity.this);
                progress.setMessage("Downloading File");
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setIndeterminate(true);
            }
            progress.show();
        }

        void dismissProgressDialog(){
            if(progress != null && progress.isShowing()){
                progress.dismiss();
            }
        }
    }


    private String getFileNameFromURL(String url){

        int index = url.lastIndexOf('/');
        if(index > 0){
            return url.substring(index+1,url.length());
        }
        return  null;
    }

    public void trustAllCertificates() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            X509Certificate[] myTrustedAnchors = new X509Certificate[0];
                            return myTrustedAnchors;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception e) {
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    isPermissionAllowed = true;
                    new DownloadFileFromURL().execute(DOCUMENT_URL);
                } else
                {
                    isPermissionAllowed = false;
                }
            }

            case REQUEST_READ_FILE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                    getFileData(selectedFileUri);
                } else
                {
                    showReadPermissionError();
                }
        }

    }

    public  String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK){
            if(data != null && data.getData() != null ) {
                boolean hasPermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_READ_FILE);
                } else {
                    selectedFileUri = data.getData();
                    getFileData(selectedFileUri);
                }
            } else {
                showFilePickError();
            }
        } else {
            showFilePickError();
        }
    }

    private void getFileData(Uri uri) {
        if(selectedFileUri != null) {
            FileUtil fileUtil = new FileUtil(MainActivity.this);
            String FilePath = fileUtil.getPath(uri);
            File file = new File(FilePath);
            // Get the number of bytes in the file
            long sizeInBytes = file.length();
            //transform in MB
            long sizeInMb = sizeInBytes / (1024 * 1024);
            if(file.exists() && sizeInMb > FILE_SIZE_LIMIT_IN_MB){
                showFileSizeError();
            } else if (file.exists()) {
                try {
                    String fileType = MimeTypeMap.getFileExtensionFromUrl(file.toURL().toString() );
                    if(isSupportedMimeTypes(fileType)){
                        String convertedFile = getStringFile(file);
                        Log.i(TAG, "getFileData: "+convertedFile);
                    } else {
                        Toast.makeText(MainActivity.this,fileType+" Filetype Not supported",Toast.LENGTH_LONG).show();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                //Log.i(TAG, "onActivityResult: " + getStringFile(file));
            }
        } else {
            showFilePickError();
        }
    }

    public String getStringFile(File f) {
        InputStream inputStream = null;
        String encodedFile= "", lastVal;
        try {
            inputStream = new FileInputStream(f.getAbsolutePath());

            byte[] buffer = new byte[10240];//specify the size to allow
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                try {
                    output64.write(buffer, 0, bytesRead);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            output64.close();
            encodedFile =  output.toString();

        }
        catch (FileNotFoundException e1 ) {
            e1.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        lastVal = encodedFile;
        return lastVal;
    }



    void showFilePickError(){
        Toast.makeText(MainActivity.this,"Error while selecting document",
                Toast.LENGTH_LONG).show();
    }

    void showReadPermissionError(){
        Toast.makeText(MainActivity.this,"Permission not granted",
                Toast.LENGTH_LONG).show();

    }

    void showFileSizeError(){
        Toast.makeText(MainActivity.this,"Document size should not be more than "+FILE_SIZE_LIMIT_IN_MB+" MB",
                Toast.LENGTH_LONG).show();

    }

    private boolean isSupportedMimeTypes(String type){

        if(type == null){
            return  false;
        }

        for (String supportedFileType : supportedFileTypes) {
            if (type.equalsIgnoreCase(supportedFileType)) {
                return true;
            }
        }
        return  false;
    }
}
