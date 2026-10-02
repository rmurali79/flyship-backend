package com.flyship.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Picks an illustrative stock photo (served from static/item-images) for a shipment's item
 * description. Used for seeded demo shipments and as the display fallback when a shipper
 * didn't upload a photo of their own.
 */
public final class ItemImages {

    public static final String BASE_PATH = "/item-images/";
    public static final String GENERIC = BASE_PATH + "parcel.jpg";

    // Keywords match whole words, optionally plural ("ring" must not hit "strings"). First match wins,
    // so more specific keywords come before broader ones ("laptop accessories" before "laptop").
    private static final Map<Pattern, String> KEYWORDS = new LinkedHashMap<>();
    static {
        put("laptop-accessories", "laptop accessories", "laptop accessory", "charger", "mouse", "keyboard", "cable");
        put("drone", "drone");
        put("laptop", "laptop", "notebook computer", "macbook");
        put("tablet", "tablet", "ipad", "kindle", "e-reader");
        put("headphones", "headphone", "earphone", "earbud", "airpods", "headset");
        put("camera-gear", "camera", "lens", "lenses", "dslr", "tripod");
        put("electronics", "electronic", "phone", "smartphone", "mobile", "gadget", "console", "speaker");
        put("watch", "watch", "watches", "clock");
        put("jewellery", "jewel", "jewellery", "jewelry", "necklace", "ring", "bracelet", "earring", "gold");
        put("perfume", "perfume", "fragrance", "cologne", "attar");
        put("skincare", "skincare", "skin care", "cosmetic", "makeup", "lotion", "cream");
        put("medicines", "medicine", "medical", "pharma", "pill", "vitamin", "supplement");
        put("baby", "baby", "infant", "diaper");
        put("toys", "toy", "doll", "lego", "plush");
        put("board-game", "board game", "puzzle", "game");
        put("sports", "sport", "racket", "badminton", "cricket", "football", "tennis", "golf", "fitness");
        put("music-parts", "music", "guitar", "violin", "instrument");
        put("invitations", "invitation", "wedding card", "greeting card");
        put("artwork", "artwork", "art", "painting", "canvas", "frame", "print");
        put("home-decor", "decor", "vase", "ceramic", "pottery", "lamp", "cushion");
        put("handicrafts", "handicraft", "craft", "carved", "wooden");
        put("spices-textiles", "spice", "masala", "textile", "saree", "sari", "fabric");
        put("fashion", "fashion", "cloth", "clothes", "clothing", "garment", "shirt", "dress", "dresses", "jacket", "apparel", "bag", "sunglasses", "sun glasses", "glasses", "eyewear");
        put("shoes", "shoe", "sneaker", "boot", "sandal", "footwear", "jordan", "airjordan", "nike", "adidas");
        put("snacks", "snack", "sweet", "chocolate", "confection", "confectionery", "food", "tea", "coffee");
        put("books", "book", "novel", "magazine");
        put("stationery", "stationery", "pen", "pencil", "diary");
        put("documents-gifts", "gift");
        put("documents", "document", "doc", "passport", "certificate", "letter", "envelope", "currency", "paper");
        put("auto-parts", "auto", "car", "vehicle", "motor", "spare part");
    }

    private static void put(String image, String... keywords) {
        for (String k : keywords) KEYWORDS.put(Pattern.compile("\\b" + Pattern.quote(k) + "s?\\b"), image);
    }

    private ItemImages() {}

    /** Returns a site-relative image path for the description; never null. */
    public static String forDescription(String description) {
        if (description == null || description.isBlank()) return GENERIC;
        String text = description.toLowerCase(Locale.ROOT);
        for (Map.Entry<Pattern, String> e : KEYWORDS.entrySet()) {
            if (e.getKey().matcher(text).find()) return BASE_PATH + e.getValue() + ".jpg";
        }
        return GENERIC;
    }
}
