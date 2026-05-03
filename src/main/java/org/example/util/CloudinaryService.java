package org.example.util;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public class CloudinaryService {

    // ── CONFIGURE THESE ──────────────────────────────────────────────────────
    private static final String CLOUD_NAME    = "dfj0hvd3g"; // replace with your cloud name
    private static final String UPLOAD_PRESET = "ml_default";      // your unsigned preset
    // ─────────────────────────────────────────────────────────────────────────

    private static final String UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static String uploadImage(File file, String folder) throws IOException, InterruptedException {
        String boundary = "----Boundary" + UUID.randomUUID().toString().replace("-", "");

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String mimeType  = Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "image/jpeg";

        // Build multipart body
        String headerStr = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                "Content-Type: " + mimeType + "\r\n\r\n";

        String presetPart = "\r\n--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n" +
                UPLOAD_PRESET + "\r\n";

        String folderPart = "";
        if (folder != null && !folder.isBlank()) {
            folderPart = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"folder\"\r\n\r\n" +
                    folder + "\r\n";
        }

        String closing = "--" + boundary + "--\r\n";

        byte[] h = headerStr.getBytes(StandardCharsets.UTF_8);
        byte[] p = presetPart.getBytes(StandardCharsets.UTF_8);
        byte[] f = folderPart.getBytes(StandardCharsets.UTF_8);
        byte[] c = closing.getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[h.length + fileBytes.length + p.length + f.length + c.length];
        int pos = 0;
        System.arraycopy(h,         0, body, pos, h.length);         pos += h.length;
        System.arraycopy(fileBytes, 0, body, pos, fileBytes.length); pos += fileBytes.length;
        System.arraycopy(p,         0, body, pos, p.length);         pos += p.length;
        System.arraycopy(f,         0, body, pos, f.length);         pos += f.length;
        System.arraycopy(c,         0, body, pos, c.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UPLOAD_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Cloudinary upload failed [" + response.statusCode() + "]: " + response.body());
        }

        return new JSONObject(response.body()).getString("secure_url");
    }
}
