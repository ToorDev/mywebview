package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.onesignal.OneSignal;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout reloadSwipe;
    private WebView webView;
    private TextView errorMessage;
    private ProgressBar loading;

    private ConnectivityReceiver connectivityReceiver;
    private static final String DEFAULT_URL = "https://github.com/";
    private String url;
    private static final String ONESIGNAL_APP_ID = "xxxxxx-xxxxxxxx-xxxxxx-xxx";
    private boolean doubleBackToExitPressedOnce = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        reloadSwipe = findViewById(R.id.reload);
        webView = findViewById(R.id.idwebview);
        errorMessage = findViewById(R.id.idTxtErreurMessage);
        loading = findViewById(R.id.idLoading);

//        reloadSwipe.setEnabled(false);
        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // OneSignal Initialization
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        // promptForPushNotifications will show the native Android notification permission prompt.
        // We recommend removing the following code and instead using an In-App Message to prompt for notification permission (See step 7)
        OneSignal.promptForPushNotifications();

        // Enable JavaScript and pop-ups
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (webView.canScrollVertically(-1)) {
                    reloadSwipe.setEnabled(false);
                } else {
                    reloadSwipe.setEnabled(true);
                }
            });
        }


        reloadSwipe.setOnRefreshListener(()->{
            if (isConnected()){
                webView.reload();
            }else{
                reloadSwipe.setRefreshing(false);
            }
        });

        //Si l'application est lance a partir d'un lien
        url = DEFAULT_URL;
        Uri data = getIntent().getData();
        if (data != null) {
            url = data.toString();
        }

        webView.setVisibility(View.GONE);
        if (isConnected()){
            webView.setVisibility(View.VISIBLE);
            errorMessage.setVisibility(View.GONE);
            webView.loadUrl(url);
        }else{
            webView.setVisibility(View.GONE);
            errorMessage.setVisibility(View.VISIBLE);
            loading.setVisibility(View.GONE);
        }

    }

    //Verifier la connexionisConnected
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Classe interne qui hérite de BroadcastReceiver pour détecter les changements de connexion Internet
    private class ConnectivityReceiver extends BroadcastReceiver {
        private void loadWebPage(String url) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (isConnected()){

                        if (url.startsWith("mailto:")) {
                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse(url));
                            PackageManager packageManager = getPackageManager();
                            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
                            boolean isIntentSafe = activities.size() > 0;
                            if(isIntentSafe) {
                                startActivity(intent);
                                return true;
                            }else{
                                // Afficher une boîte de dialogue ou un Toast pour informer l'utilisateur qu'il n'a pas d'application de messagerie installée
                                Toast.makeText(getApplicationContext(), "Aucune application de messagerie n'est installée sur cet appareil", Toast.LENGTH_LONG).show();
                            }
                        }else {
                            view.loadUrl(url);
                        }
                        return true;
                    }else {
                        Toast.makeText(getApplicationContext(), "Erreur connection", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    loading.setVisibility(View.VISIBLE);
                    errorMessage.setVisibility(View.GONE);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    loading.setVisibility(View.GONE);
                    reloadSwipe.setRefreshing(false);
                    webView.setVisibility(View.VISIBLE);
                }
                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    return super.onRenderProcessGone(view, detail);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    loading.setVisibility(View.GONE);
                    // Affiche un message d'erreur en fonction de l'erreur reçue
                    if (errorCode == ERROR_CONNECT) {
                        errorMessage.setText("Impossible de se connecter au serveur");
                    } else if (errorCode == ERROR_TIMEOUT) {
                        errorMessage.setText("Temps de chargement de la page dépassé");
                    } else {
                        errorMessage.setText("Erreur de chargement de la page");
                    }
                    errorMessage.setVisibility(View.VISIBLE);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    loading.setVisibility(View.GONE);

                    // Affiche un message d'erreur en fonction de l'erreur reçue
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (error.getErrorCode() == ERROR_CONNECT) {
                            errorMessage.setText("Impossible de se connecter au serveur");
                        } else if (error.getErrorCode() == ERROR_TIMEOUT) {
                            errorMessage.setText("Temps de chargement de la page dépassé");
                        } else {
                            errorMessage.setText("Erreur de chargement de la page");
                        }
                    }
                    errorMessage.setVisibility(View.VISIBLE);
                }

            });
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!(webView.getProgress() == 100)){
                if (isConnected()) {
                    //La connexion est de retour
                    //Vérifiez si la page n'a pas été chargée, puis chargez-la
                    // Si la connexion Internet est rétablie, on charge le lien dans le WebView
                    loadWebPage(url);
                    // On enlève le Snackbar
                    Snackbar.make(webView, "", Snackbar.LENGTH_SHORT).dismiss();
                    // On affiche un message pour informer l'utilisateur que la connexion a été rétablie
                    Snackbar.make(webView, "Connexion Internet rétablie", Snackbar.LENGTH_SHORT).show();

                } else {
                    // Si la connexion Internet est perdue, on affiche un Snackbar
                    Snackbar.make(webView, "Aucune connexion Internet", Snackbar.LENGTH_INDEFINITE).show();
                    reloadSwipe.setRefreshing(false);
                }
            }else{
                // Si la connexion Internet est rétablie, on charge le lien dans le WebView
                loadWebPage(url);
            }
        }
    }

    @Override
    protected void onResume() {
        // Enregistre un BroadcastReceiver pour détecter les changements de connexion Internet
        connectivityReceiver = new ConnectivityReceiver();
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();
        }else if(url != DEFAULT_URL){
            url = DEFAULT_URL;
        }else if (doubleBackToExitPressedOnce){
//            finish();
            super.onBackPressed();
            return;
        }else{
            doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> doubleBackToExitPressedOnce=false, 2000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Désenregistre le BroadcastReceiver lorsque l'activité est détruite
        unregisterReceiver(connectivityReceiver);
    }

}