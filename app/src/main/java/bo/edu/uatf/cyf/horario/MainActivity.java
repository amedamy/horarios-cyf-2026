package bo.edu.uatf.cyf.horario;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_SAVE_ICS = 2042;
    private WebView webView;
    private String pendingIcs;
    private String pendingFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void toast(final String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
        }

        @JavascriptInterface
        public void saveCalendar(final String icsText, final String filename) {
            pendingIcs = icsText;
            pendingFilename = (filename == null || filename.trim().isEmpty()) ? "Horario_CyF_02_2026.ics" : filename;
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/calendar");
                intent.putExtra(Intent.EXTRA_TITLE, pendingFilename);
                startActivityForResult(intent, REQ_SAVE_ICS);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SAVE_ICS || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException("No se pudo abrir el archivo");
            out.write(pendingIcs.getBytes(StandardCharsets.UTF_8));
            out.flush();
            Toast.makeText(this, "Calendario guardado. Ábrelo para importarlo con recordatorios.", Toast.LENGTH_LONG).show();
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "text/calendar");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(view); } catch (Exception ignored) { }
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo guardar el calendario: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pendingIcs = null;
            pendingFilename = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("Android");
            webView.destroy();
        }
        super.onDestroy();
    }
}
