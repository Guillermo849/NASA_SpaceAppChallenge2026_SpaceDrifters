package com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Lee un archivo de app/src/main/assets (los tests JVM se ejecutan desde la carpeta app/). */
public final class Assets {

    private Assets() {}

    public static String leer(String archivo) {
        try {
            return new String(Files.readAllBytes(Paths.get("src", "main", "assets", archivo)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
