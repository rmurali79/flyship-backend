package com.flyship.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemImagesTest {

    @ParameterizedTest
    @CsvSource({
            "Laptop, laptop",
            "Laptop Accessories, laptop-accessories",
            "Camera Gear, camera-gear",
            "Camera Drone, drone",
            "Documents & Gifts, documents-gifts",
            "Currency/Documents, documents",
            "Musical Instrument Parts, music-parts",
            "Guitar strings, music-parts",
            "Wedding Invitation Cards, invitations",
            "Snacks & Confectionery, snacks",
            "Jewellery, jewellery",
            "Auto Parts, auto-parts",
            "Two boxed smartphones, electronics",
            "Old Watches, watch",
            "Sun Glasses, fashion",
            "Airjordan Retro 11, shoes",
            "Docs, documents",
    })
    void matchesItemDescriptionToImage(String description, String expected) {
        assertEquals("/item-images/" + expected + ".jpg", ItemImages.forDescription(description));
    }

    @Test
    void fallsBackToGenericParcel() {
        assertEquals(ItemImages.GENERIC, ItemImages.forDescription(null));
        assertEquals(ItemImages.GENERIC, ItemImages.forDescription("  "));
        assertEquals(ItemImages.GENERIC, ItemImages.forDescription("Something unusual"));
    }

    @Test
    void everySeededItemResolvesToAnImageThatExists() throws Exception {
        String[] items = {"Laptop", "Camera Gear", "Documents & Gifts", "Electronics", "Handicrafts", "Books",
                "Spices & Textiles", "Fashion Items", "Watch", "Medicines", "Sports Equipment", "Toys", "Artwork",
                "Jewellery", "Perfume", "Musical Instrument Parts", "Laptop Accessories", "Snacks & Confectionery",
                "Stationery", "Baby Items", "Tablet", "Wedding Invitation Cards", "Headphones", "Shoes", "Auto Parts",
                "Currency/Documents", "Home Decor", "Skincare Products", "Board Game", "Camera Drone"};
        for (String item : items) {
            String path = ItemImages.forDescription(item);
            assertTrue(!path.equals(ItemImages.GENERIC), item + " fell back to the generic image");
            assertTrue(Files.exists(Path.of("src/main/resources/static" + path)), path + " is missing");
        }
    }
}
