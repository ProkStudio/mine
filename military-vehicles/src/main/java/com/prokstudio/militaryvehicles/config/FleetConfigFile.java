package com.prokstudio.militaryvehicles.config;

import com.prokstudio.militaryvehicles.core.FleetTuning;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;

/** Bounded UTF-8 reader. Invalid edits never replace the last good immutable snapshot. */
public final class FleetConfigFile {
    public static final int MAX_BYTES = 8192;
    private final Path file;
    private FleetTuning current = FleetTuning.DEFAULT;
    public FleetConfigFile(Path file) { this.file = Objects.requireNonNull(file).toAbsolutePath(); }
    public FleetTuning current() { return current; }
    public FleetTuning reload() throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            try {
                Files.writeString(file, "# Military Vehicles server rules. See docs/SERVER-RULES.md.\n"
                    + "# Edit, then run /militaryvehicles config reload (admin permission).\n"
                    + FleetTuning.DEFAULT.serialize(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            } catch (FileAlreadyExistsException race) { /* Read the file another writer created. */ }
        }
        byte[] bytes;
        try (var input = Files.newInputStream(file)) { bytes = input.readNBytes(MAX_BYTES + 1); }
        if (bytes.length > MAX_BYTES) throw new IOException("Settings file exceeds 8192 bytes");
        String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        FleetTuning candidate = FleetTuning.parse(text);
        current = candidate;
        return candidate;
    }
}
