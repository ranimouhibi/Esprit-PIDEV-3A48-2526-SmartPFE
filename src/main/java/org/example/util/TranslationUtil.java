package org.example.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Translation utility using MyMemory API (free, no API key required).
 * Supports: en, fr, es, de, it, ar, zh
 */
public class TranslationUtil {

    // MyMemory free API — no key needed, 5000 words/day limit
    private static final String API_URL = "https://api.mymemory.translated.net/get";

    /**
     * Translate text to the target language.
     *
     * @param text       Text to translate
     * @param targetLang Target language code (en, fr, es, de, it, ar, zh)
     * @return Translated text, or original text if translation fails
     */
    public static String translate(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) return text;

        try {
            // Detect source language first
            String sourceLang = detectLanguage(text);
            
            // If source and target are the same, return original text
            if (sourceLang.equals(targetLang)) {
                return text;
            }

            // MyMemory expects "sourceLang|targetLang" as the langpair
            String langPair = sourceLang + "|" + targetLang;

            String encodedText     = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
            String encodedLangPair = URLEncoder.encode(langPair,    StandardCharsets.UTF_8);

            String url = API_URL + "?q=" + encodedText + "&langpair=" + encodedLangPair;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SmartPFE-Desktop/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                // Check response status
                JsonObject responseData = json.getAsJsonObject("responseData");
                if (responseData != null) {
                    String translated = responseData.get("translatedText").getAsString();

                    // MyMemory returns the original text if it can't translate
                    if (translated != null && !translated.trim().isEmpty()
                            && !translated.equalsIgnoreCase(text.trim())) {
                        return translated;
                    }
                }

                // Check for error message from API
                if (json.has("responseStatus")) {
                    int status = json.get("responseStatus").getAsInt();
                    if (status != 200) {
                        System.err.println("❌ MyMemory API error status: " + status);
                        if (json.has("responseDetails")) {
                            System.err.println("   Details: " + json.get("responseDetails").getAsString());
                        }
                    }
                }
            } else {
                System.err.println("❌ MyMemory HTTP error: " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("❌ Translation error: " + e.getMessage());
        }

        // Return original text if translation failed
        return text;
    }

    /**
     * Detect the language of the text using simple heuristics.
     * Falls back to English if detection is uncertain.
     *
     * @param text Text to analyze
     * @return Detected language code (en, fr, es, de, it, ar, zh)
     */
    private static String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) return "en";
        
        String sample = text.toLowerCase().trim();
        
        // Check for Arabic characters
        if (sample.matches(".*[\\u0600-\\u06FF].*")) return "ar";
        
        // Check for Chinese characters
        if (sample.matches(".*[\\u4E00-\\u9FFF].*")) return "zh";
        
        // Check for French accents and common words
        if (sample.matches(".*[àâäéèêëïîôùûüÿœæç].*") || 
            sample.matches(".*(nouveau|nouvelle|projet|bonjour|merci|comment|pourquoi|maintenant|aujourd|demain|hier).*") ||
            sample.matches(".*(\\bje\\b|\\btu\\b|\\bil\\b|\\belle\\b|\\bnous\\b|\\bvous\\b|\\bils\\b|\\belles\\b|\\bde\\b|\\ble\\b|\\bla\\b|\\bles\\b|\\bun\\b|\\bune\\b|\\bdes\\b|\\bdu\\b|\\bau\\b|\\baux\\b|\\bce\\b|\\bcette\\b|\\bces\\b|\\bmon\\b|\\bma\\b|\\bmes\\b|\\bton\\b|\\bta\\b|\\btes\\b|\\bson\\b|\\bsa\\b|\\bses\\b|\\bnotre\\b|\\bvotre\\b|\\bleur\\b|\\bqui\\b|\\bque\\b|\\bquoi\\b|\\bdont\\b|\\boù\\b|\\bpour\\b|\\bpar\\b|\\bavec\\b|\\bsans\\b|\\bsur\\b|\\bsous\\b|\\bdans\\b|\\best\\b|\\bsont\\b|\\bété\\b|\\bêtre\\b|\\bavoir\\b|\\bfaire\\b|\\bdire\\b|\\baller\\b|\\bvenir\\b|\\bpouvoir\\b|\\bvouloir\\b|\\bdevoir\\b|\\bsavoir\\b).*")) {
            return "fr";
        }
        
        // Check for common Spanish words/patterns
        if (sample.matches(".*[áéíóúñ¿¡].*") ||
            sample.matches(".*(\\bel\\b|\\bla\\b|\\blos\\b|\\blas\\b|\\bun\\b|\\buna\\b|\\bunos\\b|\\bunas\\b|\\bde\\b|\\bdel\\b|\\bal\\b|\\bpor\\b|\\bpara\\b|\\bcon\\b|\\bsin\\b|\\bsobre\\b|\\ben\\b|\\bentre\\b|\\bhasta\\b|\\bdesde\\b|\\byo\\b|\\btú\\b|\\bél\\b|\\bella\\b|\\bnosotros\\b|\\bvosotros\\b|\\bellos\\b|\\bellas\\b|\\busted\\b|\\bustedes\\b|\\bque\\b|\\bqué\\b|\\bquién\\b|\\bcuál\\b|\\bcuándo\\b|\\bdónde\\b|\\bcómo\\b|\\bpor qué\\b|\\bser\\b|\\bestar\\b|\\bhaber\\b|\\btener\\b|\\bhacer\\b|\\bpoder\\b|\\bdecir\\b|\\bir\\b|\\bver\\b|\\bdar\\b|\\bsaber\\b|\\bquerer\\b|\\bllegar\\b|\\bpasar\\b|\\bdeber\\b|\\bponer\\b|\\bparecer\\b|\\bquedar\\b|\\bcreer\\b|\\bhablar\\b|\\bllevar\\b|\\bdejar\\b|\\bseguir\\b|\\bencontrar\\b|\\bllamar\\b|\\bvenir\\b|\\bpensar\\b|\\bsalir\\b|\\bvolver\\b|\\btomar\\b|\\bconocer\\b|\\bvivir\\b|\\bsentir\\b|\\btratar\\b|\\bmirar\\b|\\bcontar\\b|\\bempezar\\b|\\besperar\\b|\\bbuscar\\b|\\bexistir\\b|\\bentrar\\b|\\btrabajar\\b|\\bescribir\\b|\\bperder\\b|\\bproducir\\b|\\bocurrir\\b|\\bentender\\b|\\bpedir\\b|\\brecibir\\b|\\brecordar\\b|\\bterminar\\b|\\bpermitir\\b|\\baparece\\b).*")) {
            return "es";
        }
        
        // Check for common German words/patterns
        if (sample.matches(".*[äöüß].*") ||
            sample.matches(".*(\\bder\\b|\\bdie\\b|\\bdas\\b|\\bden\\b|\\bdem\\b|\\bdes\\b|\\bein\\b|\\beine\\b|\\beinen\\b|\\beinem\\b|\\beiner\\b|\\beines\\b|\\bund\\b|\\boder\\b|\\baber\\b|\\bnicht\\b|\\bich\\b|\\bdu\\b|\\ber\\b|\\bsie\\b|\\bes\\b|\\bwir\\b|\\bihr\\b|\\bist\\b|\\bsind\\b|\\bwar\\b|\\bwaren\\b|\\bhaben\\b|\\bhat\\b|\\bhatten\\b|\\bwerden\\b|\\bwird\\b|\\bwurde\\b|\\bwürde\\b|\\bkann\\b|\\bkonnte\\b|\\bmuss\\b|\\bmusste\\b|\\bsoll\\b|\\bsollte\\b|\\bwill\\b|\\bwollte\\b|\\bmag\\b|\\bmochte\\b|\\bdarf\\b|\\bdurfte\\b|\\bauf\\b|\\ban\\b|\\bin\\b|\\bzu\\b|\\bvon\\b|\\bmit\\b|\\bnach\\b|\\bbei\\b|\\baus\\b|\\bum\\b|\\bdurch\\b|\\bfür\\b|\\bwas\\b|\\bwer\\b|\\bwie\\b|\\bwo\\b|\\bwann\\b|\\bwarum\\b).*")) {
            return "de";
        }
        
        // Check for common Italian words/patterns
        if (sample.matches(".*(\\bil\\b|\\blo\\b|\\bla\\b|\\bi\\b|\\bgli\\b|\\ble\\b|\\bun\\b|\\buno\\b|\\buna\\b|\\bdi\\b|\\ba\\b|\\bda\\b|\\bin\\b|\\bcon\\b|\\bsu\\b|\\bper\\b|\\btra\\b|\\bfra\\b|\\bio\\b|\\btu\\b|\\blui\\b|\\blei\\b|\\bnoi\\b|\\bvoi\\b|\\bloro\\b|\\bche\\b|\\bchi\\b|\\bcosa\\b|\\bquale\\b|\\bquanto\\b|\\bquando\\b|\\bdove\\b|\\bcome\\b|\\bperché\\b|\\bessere\\b|\\bavere\\b|\\bfare\\b|\\bpotere\\b|\\bvolere\\b|\\bdovere\\b|\\bsapere\\b|\\bdare\\b|\\bstare\\b|\\bandare\\b|\\bvenire\\b|\\bdire\\b|\\bvedere\\b).*")) {
            return "it";
        }
        
        // Default to English
        return "en";
    }

    /**
     * Get the display name of a language code
     */
    public static String getLanguageName(String code) {
        switch (code) {
            case "en": return "English";
            case "fr": return "Français";
            case "es": return "Español";
            case "de": return "Deutsch";
            case "it": return "Italiano";
            case "ar": return "العربية";
            case "zh": return "中文";
            default:   return code;
        }
    }
}
