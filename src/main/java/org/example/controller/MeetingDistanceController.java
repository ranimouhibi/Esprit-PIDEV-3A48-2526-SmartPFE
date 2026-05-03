package org.example.controller;

import org.example.model.Meeting;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MeetingDistanceController {

    @FXML private Label meetingLocationLabel;
    @FXML private TextField originField;
    @FXML private Button calculateBtn;
    @FXML private Button closeBtn;
    @FXML private VBox resultsContainer;
    @FXML private Label statusLabel;

    private Meeting meeting;
    private double destLat, destLng;

    public void init(Meeting m) {
        this.meeting = m;
        String location = m.getLocation() != null ? m.getLocation() : "";
        meetingLocationLabel.setText("Destination: " + location);
    }

    @FXML
    public void handleCalculate() {
        String origin = originField.getText().trim();
        if (origin.isEmpty()) {
            showStatus("Please enter your starting address.", true);
            return;
        }
        String destination = meeting.getLocation() != null ? meeting.getLocation().trim() : "";
        if (destination.isEmpty()) {
            showStatus("No destination address for this meeting.", true);
            return;
        }

        calculateBtn.setDisable(true);
        resultsContainer.getChildren().clear();
        showStatus("Geocoding addresses...", false);

        new Thread(() -> {
            try {
                Platform.runLater(() -> showStatus("Locating: " + origin + "...", false));
                double[] from = geocode(origin);
                if (from == null) throw new Exception("Address not found: \"" + origin + "\"\nTry adding city/country (e.g. \"Rades, Tunisia\")");

                Platform.runLater(() -> showStatus("Locating: " + destination + "...", false));
                double[] to = geocode(destination);
                if (to == null) throw new Exception("Meeting address not found: \"" + destination + "\"");

                destLat = to[0]; destLng = to[1];
                double haversine = haversine(from[0], from[1], to[0], to[1]);

                // Estimated road distances (haversine * realistic factor)
                double drivingKm = haversine * 1.35;
                double walkingKm = haversine * 1.20;
                int drivingMin   = (int) (drivingKm / 50 * 60);  // avg 50 km/h in city
                int walkingMin   = (int) (walkingKm / 5  * 60);  // avg 5 km/h walking

                final double fromLat = from[0], fromLng = from[1];
                final double toLat   = to[0],   toLng   = to[1];

                Platform.runLater(() -> {
                    resultsContainer.getChildren().clear();

                    resultsContainer.getChildren().add(buildCard(
                        "📏 As the crow flies",
                        String.format("%.1f km", haversine),
                        "Straight-line distance",
                        "#a12c2f", "#c0392b", null));

                    resultsContainer.getChildren().add(buildCard(
                        "🚗 By Car (estimated)",
                        String.format("%.1f km", drivingKm),
                        "⏱ ~" + formatDuration(drivingMin * 60) + "  •  avg city speed",
                        "#1e3a5f", "#2c5282",
                        osmUrl(fromLat, fromLng, toLat, toLng, "fossgis_osrm_car")));

                    resultsContainer.getChildren().add(buildCard(
                        "🚶 On Foot (estimated)",
                        String.format("%.1f km", walkingKm),
                        "⏱ ~" + formatDuration(walkingMin * 60) + "  •  avg walking speed",
                        "#1a4731", "#276749",
                        osmUrl(fromLat, fromLng, toLat, toLng, "fossgis_osrm_foot")));

                    showStatus("✓ Done", false);
                    calculateBtn.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showStatus(e.getMessage(), true);
                    calculateBtn.setDisable(false);
                });
            }
        }).start();
    }

    // --- Geocoding: tries Nominatim first, then Photon as fallback ---
    private double[] geocode(String address) throws Exception {
        double[] result = geocodeNominatim(address);
        if (result == null) result = geocodePhoton(address);
        return result;
    }

    private double[] geocodeNominatim(String address) {
        try {
            // Detect if user already specified a country
            String query = address;
            boolean hasCountry = address.toLowerCase().contains("tunisia")
                || address.toLowerCase().contains("tunisie")
                || address.contains(",");

            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            // Force Tunisia context if no country specified
            String countryParam = hasCountry ? "" : "&countrycodes=tn";
            String url = "https://nominatim.openstreetmap.org/search?format=json&q="
                + encoded + "&limit=1&addressdetails=0" + countryParam;
            String json = get(url, "SmartPFE-App/1.0 (contact@smartpfe.local)");
            if (!json.contains("\"lat\"")) return null;
            int latIdx = json.indexOf("\"lat\":\"");
            int lonIdx = json.indexOf("\"lon\":\"");
            if (latIdx < 0 || lonIdx < 0) return null;
            double lat = Double.parseDouble(extractStr(json, latIdx + 7));
            double lon = Double.parseDouble(extractStr(json, lonIdx + 7));
            return new double[]{lat, lon};
        } catch (Exception e) { return null; }
    }

    private double[] geocodePhoton(String address) {
        try {
            // Append Tunisia if no country hint
            String query = address;
            if (!address.toLowerCase().contains("tunisia")
                && !address.toLowerCase().contains("tunisie")
                && !address.contains(",")) {
                query = address + ", Tunisia";
            }
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://photon.komoot.io/api/?q=" + encoded + "&limit=1&lang=fr";
            String json = get(url, "SmartPFE-App/1.0");
            int coordIdx = json.indexOf("\"coordinates\":[");
            if (coordIdx < 0) return null;
            int start = coordIdx + 15;
            int comma = json.indexOf(',', start);
            int end   = json.indexOf(']', comma);
            double lon = Double.parseDouble(json.substring(start, comma).trim());
            double lat = Double.parseDouble(json.substring(comma + 1, end).trim());
            return new double[]{lat, lon};
        } catch (Exception e) { return null; }
    }


    private String get(String urlStr, String userAgent) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String extractStr(String json, int pos) {
        int end = json.indexOf('"', pos);
        return json.substring(pos, end);
    }

    private String extractNum(String json, int pos) {
        int end = pos;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
        return json.substring(pos, end);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private VBox buildCard(String title, String value, String sub,
                            String color1, String color2, String link) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color:linear-gradient(to right," + color1 + "," + color2 + ");"
            + "-fx-background-radius:10;");

        Label t = new Label(title);
        t.setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-font-size:13px;-fx-font-weight:bold;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill:white;-fx-font-size:22px;-fx-font-weight:bold;");
        Label s = new Label(sub);
        s.setStyle("-fx-text-fill:rgba(255,255,255,0.75);-fx-font-size:12px;");
        card.getChildren().addAll(t, v, s);

        if (link != null) {
            Button btn = new Button("🗺️ Open Route on Map");
            btn.setStyle("-fx-background-color:rgba(255,255,255,0.2);-fx-text-fill:white;"
                + "-fx-background-radius:6;-fx-cursor:hand;-fx-padding:6 14;-fx-font-size:11px;");
            btn.setOnAction(e -> {
                try { java.awt.Desktop.getDesktop().browse(new java.net.URI(link)); }
                catch (Exception ex) { ex.printStackTrace(); }
            });
            card.getChildren().add(btn);
        }
        return card;
    }

    private VBox buildErrorCard(String title, String message) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color:#374151;-fx-background-radius:10;");
        Label t = new Label(title);
        t.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;");
        Label m = new Label(message);
        m.setStyle("-fx-text-fill:rgba(255,255,255,0.6);-fx-font-size:12px;");
        card.getChildren().addAll(t, m);
        return card;
    }

    private String osmUrl(double lat1, double lon1, double lat2, double lon2, String engine) {
        return String.format(
            "https://www.openstreetmap.org/directions?from=%.6f,%.6f&to=%.6f,%.6f&engine=%s",
            lat1, lon1, lat2, lon2, engine);
    }

    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        if (h > 0) return h + "h " + m + "min";
        return m + " min";
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError
            ? "-fx-text-fill:#e74c3c;-fx-font-size:12px;"
            : "-fx-text-fill:#8b8fa8;-fx-font-size:12px;");
    }

    @FXML public void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }
}
