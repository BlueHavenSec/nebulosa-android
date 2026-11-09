package com.nebula.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 10, 15);
    private static final int PANEL = Color.rgb(20, 22, 31);
    private static final int PANEL_2 = Color.rgb(30, 32, 43);
    private static final int TEXT = Color.rgb(244, 244, 247);
    private static final int MUTED = Color.rgb(156, 158, 171);
    private static final int ACCENT = Color.rgb(255, 107, 74);
    private static final String VIDSRC_BASE_URL = "https://vidsrc.sh/embed";

    private final List<MediaItem> catalog = new ArrayList<>(Arrays.asList(
            new MediaItem("Interstellar", "2014", "Película", "157336", false, 0xff4d5d8d),
            new MediaItem("Dune: Parte Dos", "2024", "Película", "693134", false, 0xffa36a43),
            new MediaItem("The Batman", "2022", "Película", "414906", false, 0xff384b62),
            new MediaItem("Inception", "2010", "Película", "27205", false, 0xff6e4a6b),
            new MediaItem("Breaking Bad", "2008", "Serie", "1399", true, 0xff5c7c63),
            new MediaItem("The Last of Us", "2023", "Serie", "100088", true, 0xff6b5b4e),
            new MediaItem("Severance", "2022", "Serie", "95396", true, 0xff4e647b),
            new MediaItem("Stranger Things", "2016", "Serie", "66732", true, 0xff713f54)
    ));

    private FrameLayout screen;
    private LinearLayout homeContent;
    private EditText searchField;
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int currentSeason = 1;
    private int currentEpisode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildShell();
        showHome();
        loadTmdbCatalog();
    }

    private void buildShell() {
        screen = new FrameLayout(this);
        screen.setBackgroundColor(BG);
        setContentView(screen);
    }

    private void showHome() {
        screen.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        homeContent = new LinearLayout(this);
        homeContent.setOrientation(LinearLayout.VERTICAL);
        homeContent.setPadding(dp(20), dp(18), dp(20), dp(34));
        scroll.addView(homeContent);
        screen.addView(scroll, full());

        addTopBar(homeContent, false, null);
        addSearch(homeContent);
        addCategoryChips(homeContent);
        addHero(homeContent);
        addCatalogSections(homeContent, "Tendencias", catalog.subList(0, 4));
        addCatalogSections(homeContent, "Series para maratonear", catalog.subList(4, catalog.size()));
        TextView attribution = text("Datos e imágenes proporcionados por TMDB. Nébula no está afiliada ni respaldada por TMDB.", 11, MUTED, false);
        attribution.setPadding(0, dp(24), 0, 0);
        homeContent.addView(attribution, match(dp(42)));
    }

    private void addTopBar(LinearLayout parent, boolean back, View.OnClickListener listener) {
        LinearLayout bar = row();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        if (back) {
            ImageButton backButton = new ImageButton(this);
            backButton.setImageResource(android.R.drawable.ic_media_previous);
            backButton.setColorFilter(TEXT);
            backButton.setBackgroundColor(Color.TRANSPARENT);
            backButton.setContentDescription("Volver");
            backButton.setOnClickListener(listener);
            bar.addView(backButton, size(44, 44));
        }

        TextView logo = text("NÉBULA", 20, TEXT, true);
        logo.setLetterSpacing(0.16f);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        logoParams.gravity = Gravity.CENTER_VERTICAL;
        bar.addView(logo, logoParams);

        TextView profile = text("●", 24, ACCENT, true);
        profile.setGravity(Gravity.CENTER);
        profile.setContentDescription("Perfil");
        bar.addView(profile, size(44, 44));
        parent.addView(bar, match(dp(48)));
    }

    private void addSearch(LinearLayout parent) {
        searchField = new EditText(this);
        searchField.setSingleLine(true);
        searchField.setHint("Buscar películas y series");
        searchField.setHintTextColor(MUTED);
        searchField.setTextColor(TEXT);
        searchField.setTextSize(15);
        searchField.setPadding(dp(16), 0, dp(16), 0);
        searchField.setBackground(round(PANEL_2, 18));
        LinearLayout.LayoutParams params = match(dp(52));
        params.topMargin = dp(12);
        parent.addView(searchField, params);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshSearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void addCategoryChips(LinearLayout parent) {
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = row();
        chips.setPadding(0, dp(14), 0, dp(4));
        String[] labels = {"Inicio", "Películas", "Series", "Mi lista"};
        for (int i = 0; i < labels.length; i++) {
            TextView chip = text(labels[i], 13, i == 0 ? BG : TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), 0, dp(16), 0);
            chip.setBackground(round(i == 0 ? ACCENT : PANEL_2, 22));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(92), dp(38));
            p.rightMargin = dp(8);
            chips.addView(chip, p);
        }
        scroller.addView(chips);
        parent.addView(scroller, match(dp(58)));
    }

    private void addHero(LinearLayout parent) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(24), dp(22), dp(20));
        hero.setBackground(heroBackground());
        TextView eyebrow = text("NÉBULA ORIGINALS", 11, 0xffffc1b5, true);
        eyebrow.setLetterSpacing(0.12f);
        hero.addView(eyebrow, match(dp(28)));
        TextView title = text("Historias que\nse quedan contigo", 30, TEXT, true);
        hero.addView(title, match(dp(82)));
        TextView copy = text("Descubre tu próxima obsesión en una experiencia hecha para ver sin prisa.", 14, 0xffd7d5db, false);
        copy.setMaxLines(2);
        hero.addView(copy, match(dp(48)));
        Button action = button("Ver ahora  ▶", ACCENT, BG);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(dp(142), dp(46));
        actionParams.topMargin = dp(8);
        hero.addView(action, actionParams);
        action.setOnClickListener(v -> openPlayer(catalog.get(0)));
        LinearLayout.LayoutParams heroParams = match(dp(240));
        heroParams.topMargin = dp(12);
        parent.addView(hero, heroParams);
    }

    private void addCatalogSections(LinearLayout parent, String heading, List<MediaItem> items) {
        TextView title = text(heading, 20, TEXT, true);
        LinearLayout.LayoutParams titleParams = match(dp(32));
        titleParams.topMargin = dp(24);
        parent.addView(title, titleParams);
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout cards = row();
        for (MediaItem item : items) cards.addView(createCard(item), new LinearLayout.LayoutParams(dp(148), dp(246)));
        scroller.addView(cards);
        parent.addView(scroller, match(dp(254)));
    }

    private View createCard(MediaItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, 0, dp(10), 0);
        FrameLayout posterFrame = new FrameLayout(this);
        posterFrame.setBackground(posterBackground(item.color));
        ImageView posterImage = new ImageView(this);
        posterImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        posterFrame.addView(posterImage, new FrameLayout.LayoutParams(-1, -1));
        TextView posterTitle = text(item.title, 16, TEXT, true);
        posterTitle.setGravity(Gravity.BOTTOM | Gravity.START);
        posterTitle.setPadding(dp(12), dp(12), dp(12), dp(12));
        posterTitle.setMaxLines(3);
        posterTitle.setBackgroundColor(0x66000000);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(-1, dp(72), Gravity.BOTTOM);
        posterFrame.addView(posterTitle, titleParams);
        card.addView(posterFrame, match(dp(194)));
        if (item.posterPath != null && !item.posterPath.isEmpty()) loadPoster(item.posterPath, posterImage);
        TextView name = text(item.title, 14, TEXT, true);
        name.setMaxLines(1);
        card.addView(name, match(dp(28)));
        TextView meta = text(item.year + "  ·  " + item.type, 12, MUTED, false);
        card.addView(meta, match(dp(22)));
        card.setOnClickListener(v -> openPlayer(item));
        card.setContentDescription("Abrir " + item.title);
        return card;
    }

    private void refreshSearch(String query) {
        if (homeContent == null) return;
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            showHome();
            return;
        }
        homeContent.removeViews(3, Math.max(0, homeContent.getChildCount() - 3));
        List<MediaItem> filtered = new ArrayList<>();
        for (MediaItem item : catalog) if (item.title.toLowerCase(Locale.ROOT).contains(normalized)) filtered.add(item);
        TextView heading = text(filtered.isEmpty() ? "Sin resultados" : "Resultados para \"" + query.trim() + "\"", 20, TEXT, true);
        LinearLayout.LayoutParams headingParams = match(dp(34));
        headingParams.topMargin = dp(24);
        homeContent.addView(heading, headingParams);
        if (!filtered.isEmpty()) {
            HorizontalScrollView scroller = new HorizontalScrollView(this);
            scroller.setHorizontalScrollBarEnabled(false);
            LinearLayout cards = row();
            for (MediaItem item : filtered) cards.addView(createCard(item), new LinearLayout.LayoutParams(dp(148), dp(246)));
            scroller.addView(cards);
            homeContent.addView(scroller, match(dp(254)));
        } else {
            TextView empty = text("Prueba con otro título.", 14, MUTED, false);
            empty.setPadding(0, dp(12), 0, 0);
            homeContent.addView(empty, match(dp(48)));
        }
    }

    private void loadTmdbCatalog() {
        if (BuildConfig.TMDB_API_KEY.isEmpty()) return;
        networkExecutor.execute(() -> {
            try {
                List<MediaItem> remote = new ArrayList<>();
                appendTmdbResults(remote, tmdbGet("movie/popular"), false, 0xff4d5d8d);
                appendTmdbResults(remote, tmdbGet("tv/popular"), true, 0xff5c7c63);
                if (remote.size() < 4) return;
                mainHandler.post(() -> {
                    catalog.clear();
                    catalog.addAll(remote);
                    showHome();
                });
            } catch (Exception ignored) {
                // El catálogo demo se mantiene si TMDB no está disponible.
            }
        });
    }

    private void appendTmdbResults(List<MediaItem> target, String json, boolean series, int fallbackColor) throws Exception {
        if (json == null || json.isEmpty()) return;
        JSONArray results = new JSONObject(json).optJSONArray("results");
        if (results == null) return;
        int added = 0;
        for (int i = 0; i < results.length() && added < 4; i++) {
            JSONObject item = results.optJSONObject(i);
            if (item == null) continue;
            String title = item.optString(series ? "name" : "title", "Sin título");
            String date = item.optString(series ? "first_air_date" : "release_date", "");
            String year = date.length() >= 4 ? date.substring(0, 4) : "—";
            String posterPath = item.optString("poster_path", "");
            int color = Color.rgb(55 + (added * 17) % 90, 66 + (added * 13) % 80, 92 + (added * 19) % 90);
            if (posterPath.isEmpty()) color = fallbackColor;
            target.add(new MediaItem(title, year, series ? "Serie" : "Película",
                    String.valueOf(item.optInt("id")), series, color, posterPath,
                    item.optString("overview", "")));
            added++;
        }
    }

    private String tmdbGet(String endpoint) throws Exception {
        String separator = endpoint.contains("?") ? "&" : "?";
        String requestUrl = "https://api.themoviedb.org/3/" + endpoint + separator
                + "api_key=" + URLEncoder.encode(BuildConfig.TMDB_API_KEY, "UTF-8")
                + "&language=es-ES";
        HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String body = readBody(stream);
            if (responseCode < 200 || responseCode >= 300) throw new Exception("TMDB HTTP " + responseCode);
            return body;
        } finally {
            connection.disconnect();
        }
    }

    private String readBody(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        return body.toString();
    }

    private void loadPoster(String posterPath, ImageView target) {
        target.setTag(posterPath);
        networkExecutor.execute(() -> {
            try {
                URL url = new URL("https://image.tmdb.org/t/p/w500" + posterPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(10000);
                Bitmap bitmap;
                try (InputStream stream = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(stream);
                } finally {
                    connection.disconnect();
                }
                if (bitmap != null) {
                    mainHandler.post(() -> {
                        if (posterPath.equals(target.getTag())) target.setImageBitmap(bitmap);
                    });
                }
            } catch (Exception ignored) {
                // La tarjeta conserva su fondo de color si la imagen no está disponible.
            }
        });
    }

    private void openPlayer(MediaItem item) {
        currentSeason = 1;
        currentEpisode = 1;
        screen.removeAllViews();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(20));
        root.setBackgroundColor(BG);
        screen.addView(root, full());
        addTopBar(root, true, v -> showHome());

        TextView heading = text(item.title, 24, TEXT, true);
        LinearLayout.LayoutParams headingParams = match(dp(42));
        headingParams.topMargin = dp(12);
        root.addView(heading, headingParams);
        TextView subtitle = text(item.year + "  ·  " + item.type + "  ·  HD", 13, MUTED, false);
        root.addView(subtitle, match(dp(26)));

        WebView player = new WebView(this);
        player.setBackgroundColor(Color.BLACK);
        WebSettings settings = player.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        player.setWebViewClient(new AdBlockingWebViewClient());
        player.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                // VidSrc se mantiene en el mismo reproductor; los popunders no abren ventanas nuevas.
                return false;
            }
        });
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        LinearLayout.LayoutParams playerParams = match(dp(218));
        playerParams.topMargin = dp(10);
        root.addView(player, playerParams);
        player.loadUrl(embedUrl(item));

        if (item.series) addEpisodeControls(root, item, player);
        TextView note = text("El reproductor se carga desde VidSrc. La disponibilidad depende del proveedor y de los derechos del contenido en tu región.", 12, MUTED, false);
        note.setPadding(dp(2), dp(16), dp(2), 0);
        root.addView(note, match(dp(62)));
    }

    private void addEpisodeControls(LinearLayout root, MediaItem item, WebView player) {
        LinearLayout controls = row();
        controls.setGravity(Gravity.CENTER_VERTICAL);
        TextView episode = text("Temporada 1  ·  Episodio 1", 13, TEXT, true);
        controls.addView(episode, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button previous = button("‹", PANEL_2, TEXT);
        Button next = button("›", PANEL_2, TEXT);
        controls.addView(previous, size(46, 42));
        LinearLayout.LayoutParams nextParams = size(46, 42);
        nextParams.leftMargin = dp(6);
        controls.addView(next, nextParams);
        previous.setOnClickListener(v -> {
            if (currentEpisode > 1) currentEpisode--;
            episode.setText("Temporada " + currentSeason + "  ·  Episodio " + currentEpisode);
            player.loadUrl(embedUrl(item));
        });
        next.setOnClickListener(v -> {
            currentEpisode++;
            episode.setText("Temporada " + currentSeason + "  ·  Episodio " + currentEpisode);
            player.loadUrl(embedUrl(item));
        });
        root.addView(controls, match(dp(52)));
    }

    private String embedUrl(MediaItem item) {
        if (item.series) return VIDSRC_BASE_URL + "/tv/" + item.id + "/" + currentSeason + "/" + currentEpisode;
        return VIDSRC_BASE_URL + "/movie/" + item.id;
    }

    @Override
    public void onBackPressed() {
        if (homeContent != null && screen.getChildCount() > 0 && screen.getChildAt(0) instanceof LinearLayout) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    private LinearLayout row() { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); return row; }
    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }
    private Button button(String value, int background, int color) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(color);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(round(background, 18));
        return button;
    }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private GradientDrawable posterBackground(int color) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color, darken(color)}); d.setCornerRadius(dp(16)); return d; }
    private GradientDrawable heroBackground() { return new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xff573c4e, 0xff202b45, 0xff13151e}); }
    private int darken(int color) { return Color.rgb((int)(Color.red(color) * .45), (int)(Color.green(color) * .45), (int)(Color.blue(color) * .45)); }
    private LinearLayout.LayoutParams full() { return new LinearLayout.LayoutParams(-1, -1); }
    private LinearLayout.LayoutParams match(int height) { return new LinearLayout.LayoutParams(-1, height); }
    private LinearLayout.LayoutParams size(int width, int height) { return new LinearLayout.LayoutParams(dp(width), dp(height)); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private static final class AdBlockingWebViewClient extends WebViewClient {
        private static final Set<String> BLOCKED_HOSTS = new HashSet<>(Arrays.asList(
                "doubleclick.net", "googlesyndication.com", "googleadservices.com",
                "adservice.google.com", "googletagmanager.com", "popads.net",
                "popcash.net", "propellerads.com", "exoclick.com", "trafficjunky.net",
                "adnxs.com", "adskeeper.co.uk", "onclickalgo.com", "juicyads.com",
                "ad-maven.com", "hilltopads.net", "richpush.co", "pushwelcome.com",
                "monetag.com", "monetag.org", "betweendigital.com", "criteo.com",
                "outbrain.com", "taboola.com", "mgid.com", "media.net", "mads.amazon-adsystem.com"
        ));

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return filter(request.getUrl().toString());
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return filter(url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (isBlocked(url)) return true;
            return request.isForMainFrame() && !isTrustedPlayerHost(request.getUrl().getHost());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (isBlocked(url)) return true;
            return !isTrustedPlayerHost(Uri.parse(url).getHost());
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.evaluateJavascript("(function(){"
                    + "var bad=/((^|[._\\/-])(ad|ads|advert|banner|popup|popunder|clickunder|sponsor|monetag|onclck)([._\\/-]|$))/i;"
                    + "function clean(){document.querySelectorAll('iframe,object,embed,[id],[class]').forEach(function(e){"
                    + "var s=(e.id||'')+' '+String(e.className||'')+' '+(e.src||e.data||'');"
                    + "if(bad.test(s)&&!/(video|player|stream)/i.test(s)){e.remove();}});}"
                    + "clean();setInterval(clean,1000);})()", null);
        }

        private WebResourceResponse filter(String url) {
            if (!isBlocked(url)) return null;
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
        }

        private boolean isBlocked(String url) {
            try {
                Uri uri = Uri.parse(url);
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
                for (String blocked : BLOCKED_HOSTS) {
                    if (host.equals(blocked) || host.endsWith("." + blocked)) return true;
                }
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                if (!scheme.equals("http") && !scheme.equals("https")) return true;
                String target = url.toLowerCase(Locale.ROOT);
                return target.contains("/popunder") || target.contains("/pop-up")
                        || target.contains("/popup") || target.contains("/adserver/")
                        || target.contains("/banner-ad/") || target.contains("/advertisement/")
                        || target.contains("/ads/") || target.contains("/ad/")
                        || target.contains("ad_click") || target.contains("clickunder")
                        || target.contains("/push/") || target.contains("/notification/");
            } catch (Exception ignored) {
                return false;
            }
        }

        private boolean isTrustedPlayerHost(String host) {
            if (host == null) return false;
            String normalized = host.toLowerCase(Locale.ROOT);
            return normalized.equals("vidsrc.sh") || normalized.endsWith(".vidsrc.sh")
                    || normalized.equals("vidsrc.to") || normalized.endsWith(".vidsrc.to")
                    || normalized.equals("vidsrc.me") || normalized.endsWith(".vidsrc.me")
                    || normalized.equals("vidsrc.xyz") || normalized.endsWith(".vidsrc.xyz");
        }
    }

    private static final class MediaItem {
        final String title, year, type, id, posterPath, overview;
        final boolean series;
        final int color;
        MediaItem(String title, String year, String type, String id, boolean series, int color) {
            this(title, year, type, id, series, color, "", "");
        }
        MediaItem(String title, String year, String type, String id, boolean series, int color, String posterPath, String overview) {
            this.title = title; this.year = year; this.type = type; this.id = id; this.series = series;
            this.color = color; this.posterPath = posterPath; this.overview = overview;
        }
    }
}
