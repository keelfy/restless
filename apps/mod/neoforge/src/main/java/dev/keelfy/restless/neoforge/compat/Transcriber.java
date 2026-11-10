package dev.keelfy.restless.neoforge.compat;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * Speech to text for the spirit box, with Vosk. Vosk (native code, 25 MB) and its models (about 45 MB a language) are
 * not in the mod jar: the server fetches them the first time someone speaks near a spirit box, into {@code <game
 * dir>/restless/voice}, and checks each against a pinned SHA-256. Until a language is ready, or where Vosk has no
 * native build (it has Linux x64, Windows x64 and macOS), phrases are answered as not made out.
 */
final class Transcriber {
    private static final Logger LOG = LogUtils.getLogger();

    /** One Vosk model; small ones, as they load in seconds and answer within a phrase's pause. */
    private enum Model {
        EN("vosk-model-small-en-us-0.15", "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498"),
        RU("vosk-model-small-ru-0.22", "961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4");

        final String name;
        final String sha256;

        Model(String name, String sha256) {
            this.name = name;
            this.sha256 = sha256;
        }

        static Model of(String language) {
            return language.startsWith("ru") ? RU : EN;
        }
    }

    private static final String VOSK_URL = "https://repo1.maven.org/maven2/com/alphacephei/vosk/0.3.45/vosk-0.3.45.jar";
    private static final String VOSK_SHA256 = "9e38e96e4448a41d889bb254b2f8424945554945c6e77e1cd97a3d8633fac2ba";
    private static final String JNA_URL = "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.17.0/jna-5.17.0.jar";
    private static final String JNA_SHA256 = "b3a9408e7c51e08ef0e3bfcc08f443f6ec0f6191ba8cd7c18d53d2b22e5bdbc0";
    private static final String MODEL_URL = "https://alphacephei.com/vosk/models/";

    /** Fetches and loads; slow, so it never holds up the phrases of a language that is ready. */
    private static final ExecutorService LOADER = daemon("Restless voice loader");
    /** Makes phrases out, one at a time. */
    private static final ExecutorService WORKER = daemon("Restless voice");
    /** Loaded models; a language is in {@link #STARTED} from its first phrase, so it is fetched once. */
    private static final Map<Model, Object> READY = new ConcurrentHashMap<>();

    private static final Map<Model, Boolean> STARTED = new ConcurrentHashMap<>();

    /** Vosk's classes, loaded on {@link #LOADER} before the first model. */
    private static volatile Vosk vosk;

    private record Vosk(Constructor<?> model, Constructor<?> recognizer, Method accept, Method result) {}

    private Transcriber() {}

    /**
     * Makes out a phrase of mono samples spoken by a player whose game is in {@code language} (e.g. {@code ru_ru}) and
     * hands the words, or an empty line, to {@code done}: on a voice thread, or right away while the language loads.
     */
    static void transcribe(short[] samples, int rate, String language, Consumer<String> done) {
        Model model = Model.of(language);
        Object loaded = READY.get(model);
        if (loaded == null) {
            if (STARTED.putIfAbsent(model, true) == null) {
                LOADER.execute(() -> load(model));
            }
            done.accept("");
            return;
        }
        WORKER.execute(() -> done.accept(text(loaded, samples, rate)));
    }

    private static String text(Object model, short[] samples, int rate) {
        try (AutoCloseable recognizer = (AutoCloseable) vosk.recognizer().newInstance(model, (float) rate)) {
            vosk.accept().invoke(recognizer, samples, samples.length);
            String json = (String) vosk.result().invoke(recognizer);
            return JsonParser.parseString(json).getAsJsonObject().get("text").getAsString();
        } catch (Exception e) {
            LOG.warn("Restless could not make out a voice phrase", e);
            return "";
        }
    }

    private static void load(Model model) {
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("restless").resolve("voice");
            Files.createDirectories(dir);
            if (vosk == null) {
                vosk = classes(dir);
            }
            Path modelDir = dir.resolve(model.name);
            if (!Files.isDirectory(modelDir)) {
                Path zip = fetch(MODEL_URL + model.name + ".zip", model.sha256, dir.resolve(model.name + ".zip"));
                unzip(zip, dir, modelDir);
                Files.delete(zip);
            }
            READY.put(model, vosk.model().newInstance(modelDir.toString()));
            LOG.info("Restless voice: {} ready", model.name);
        } catch (Throwable e) {
            // No native build for this machine, no network: phrases stay unheard.
            LOG.warn("Restless voice: speech recognition is off, {} could not be loaded", model.name, e);
        }
    }

    private static Vosk classes(Path dir) throws Exception {
        ClassLoader parent = Transcriber.class.getClassLoader();
        List<URL> jars = new ArrayList<>();
        jars.add(fetch(VOSK_URL, VOSK_SHA256, dir.resolve("vosk-0.3.45.jar"))
                .toUri()
                .toURL());
        try {
            Class.forName("com.sun.jna.Native", false, parent);
        } catch (ClassNotFoundException e) {
            jars.add(fetch(JNA_URL, JNA_SHA256, dir.resolve("jna-5.17.0.jar"))
                    .toUri()
                    .toURL());
        }
        ClassLoader loader = new URLClassLoader(jars.toArray(URL[]::new), parent);
        Class<?> lib = Class.forName("org.vosk.LibVosk", true, loader);
        lib.getMethod("vosk_set_log_level", int.class).invoke(null, -1);
        Class<?> model = Class.forName("org.vosk.Model", true, loader);
        Class<?> recognizer = Class.forName("org.vosk.Recognizer", true, loader);
        return new Vosk(
                model.getConstructor(String.class),
                recognizer.getConstructor(model, float.class),
                recognizer.getMethod("acceptWaveForm", short[].class, int.class),
                recognizer.getMethod("getFinalResult"));
    }

    /** The file at {@code target}, downloaded first when missing; a download with another hash is thrown away. */
    private static Path fetch(String url, String sha256, Path target) throws IOException, InterruptedException {
        if (Files.isRegularFile(target)) {
            return target;
        }
        LOG.info("Restless voice: downloading {}", url);
        Path part = target.resolveSibling(target.getFileName() + ".part");
        try (HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            HttpResponse<Path> response =
                    http.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofFile(part));
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " for " + url);
            }
        }
        String got = sha256(part);
        if (!got.equals(sha256)) {
            Files.delete(part);
            throw new IOException("SHA-256 " + got + " for " + url + ", expected " + sha256);
        }
        Files.move(part, target, StandardCopyOption.ATOMIC_MOVE);
        return target;
    }

    private static String sha256(Path file) throws IOException {
        try (DigestInputStream in =
                new DigestInputStream(Files.newInputStream(file), MessageDigest.getInstance("SHA-256"))) {
            in.transferTo(java.io.OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(in.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Unpacks a model zip, whose files sit in one folder named as the model, into {@code target}. */
    private static void unzip(Path zip, Path dir, Path target) throws IOException {
        Path temp = dir.resolve(target.getFileName() + ".unpacking");
        try (InputStream file = Files.newInputStream(zip);
                ZipInputStream in = new ZipInputStream(file)) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                Path out = temp.resolve(entry.getName()).normalize();
                if (!out.startsWith(temp)) {
                    throw new IOException("Zip entry outside the model folder: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Files.move(temp.resolve(target.getFileName()), target, StandardCopyOption.ATOMIC_MOVE);
        try (var rest = Files.walk(temp)) {
            rest.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
        }
    }

    private static ExecutorService daemon(String name) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        });
    }
}
