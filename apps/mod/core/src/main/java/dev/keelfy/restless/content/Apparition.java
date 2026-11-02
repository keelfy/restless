package dev.keelfy.restless.content;

import java.util.List;
import java.util.Random;

/**
 * How one spirit looks on film. Its type gives the build (hooded, hunched, horned...) and its seed shifts the details,
 * so two spirits of a type are kin but never twins, and the same spirit always looks the same.
 *
 * <p>All lengths are in figure heights: {@code u} across from the centre line, {@code t} down from the top.
 *
 * @param headR size of the head
 * @param headY height of the head's centre
 * @param bodyTop where the body starts
 * @param half half width of the body at its top
 * @param flare how much wider the body gets per height going down
 * @param ripple how much the edges of the body wave
 * @param rippleFreq how fast they wave
 * @param fade how fast the body thins towards the feet
 * @param halo strength of the glow around the middle
 * @param eyeGap half the distance between the eyes
 * @param eyeR size of the eyes
 * @param mouthW half width of the mouth, 0 for none
 * @param mouthH half height of the mouth
 * @param lean how far the top leans sideways
 * @param phase where in its wave the ripple starts
 * @param limbs arms, horns, hair and the like, each mirrored on both sides
 */
record Apparition(
        double headR,
        double headY,
        double bodyTop,
        double half,
        double flare,
        double ripple,
        double rippleFreq,
        double fade,
        double halo,
        double eyeGap,
        double eyeR,
        double mouthW,
        double mouthH,
        double lean,
        double phase,
        List<Limb> limbs) {
    /** A rounded stroke from ({@code x0}, {@code t0}) to ({@code x1}, {@code t1}), {@code w} thick. */
    record Limb(double x0, double t0, double x1, double t1, double w) {}

    private static final Apparition SHADE =
            new Apparition(0.1, 0.12, 0.19, 0.07, 0.2, 0.025, 14, 0.6, 0.25, 0.038, 0.022, 0, 0, 0, 0, List.of());

    static Apparition of(String spirit, long seed) {
        Apparition base = base(spirit);
        Random random = new Random(seed);
        return new Apparition(
                base.headR * vary(random, 0.12),
                base.headY,
                base.bodyTop,
                base.half * vary(random, 0.18),
                base.flare * vary(random, 0.2),
                base.ripple * vary(random, 0.3),
                base.rippleFreq * vary(random, 0.2),
                base.fade * vary(random, 0.15),
                base.halo * vary(random, 0.3),
                base.eyeGap * vary(random, 0.2),
                base.eyeR * vary(random, 0.2),
                base.mouthW * vary(random, 0.2),
                base.mouthH * vary(random, 0.2),
                base.lean + (random.nextDouble() - 0.5) * 0.08,
                random.nextDouble() * Math.PI * 2,
                base.limbs.stream()
                        .map(l -> new Limb(
                                l.x0,
                                l.t0,
                                l.x1 * vary(random, 0.15),
                                l.t1 + (random.nextDouble() - 0.5) * 0.08,
                                l.w * vary(random, 0.2)))
                        .toList());
    }

    private static double vary(Random random, double spread) {
        return 1 + (random.nextDouble() - 0.5) * 2 * spread;
    }

    private static Apparition base(String spirit) {
        return switch (spirit) {
            // Tall and thin, more mist than body.
            case "phantom" ->
                new Apparition(0.085, 0.1, 0.17, 0.05, 0.12, 0.04, 20, 0.35, 0.4, 0.034, 0.02, 0, 0, 0, 0, List.of());
            // Almost a person: even shoulders, arms at its sides.
            case "mimic" ->
                new Apparition(
                        0.1,
                        0.12,
                        0.2,
                        0.1,
                        0.03,
                        0.008,
                        10,
                        0.5,
                        0.15,
                        0.03,
                        0.018,
                        0.02,
                        0.008,
                        0.02,
                        0,
                        List.of(new Limb(0.1, 0.24, 0.13, 0.55, 0.035)));
            // Hooded, widening to a robe, arms held out.
            case "wraith" ->
                new Apparition(
                        0.14,
                        0.14,
                        0.24,
                        0.1,
                        0.3,
                        0.03,
                        12,
                        0.5,
                        0.3,
                        0.04,
                        0.02,
                        0,
                        0,
                        0,
                        0,
                        List.of(new Limb(0.1, 0.3, 0.42, 0.4, 0.03)));
            // Hunched, arms hanging nearly to the ground.
            case "mara" ->
                new Apparition(
                        0.1,
                        0.2,
                        0.28,
                        0.08,
                        0.15,
                        0.02,
                        14,
                        0.5,
                        0.2,
                        0.035,
                        0.02,
                        0,
                        0,
                        0.16,
                        0,
                        List.of(new Limb(0.08, 0.34, 0.2, 0.9, 0.025)));
            // Hair streaming up and out, mouth open in a scream.
            case "banshee" ->
                new Apparition(
                        0.09,
                        0.14,
                        0.23,
                        0.05,
                        0.14,
                        0.035,
                        18,
                        0.5,
                        0.4,
                        0.036,
                        0.025,
                        0.05,
                        0.05,
                        0,
                        0,
                        List.of(
                                new Limb(0.06, 0.08, 0.3, -0.1, 0.02),
                                new Limb(0.07, 0.12, 0.36, 0.08, 0.02),
                                new Limb(0.07, 0.16, 0.26, 0.4, 0.02)));
            // Broad, heavy, still; big hollow eyes.
            case "revenant" ->
                new Apparition(
                        0.11,
                        0.13,
                        0.22,
                        0.14,
                        0.05,
                        0.005,
                        8,
                        0.7,
                        0.1,
                        0.045,
                        0.03,
                        0,
                        0,
                        0,
                        0,
                        List.of(new Limb(0.14, 0.26, 0.2, 0.62, 0.05)));
            // Small and round-headed, with eyes too big.
            case "lost_one" ->
                new Apparition(
                        0.15,
                        0.17,
                        0.3,
                        0.06,
                        0.12,
                        0.02,
                        12,
                        0.6,
                        0.3,
                        0.06,
                        0.04,
                        0,
                        0,
                        0,
                        0,
                        List.of(new Limb(0.07, 0.36, 0.14, 0.58, 0.025)));
            // Torn apart: arms flung up in every direction.
            case "poltergeist" ->
                new Apparition(
                        0.1,
                        0.12,
                        0.2,
                        0.07,
                        0.1,
                        0.05,
                        22,
                        0.8,
                        0.25,
                        0.038,
                        0.022,
                        0.03,
                        0.02,
                        0,
                        0,
                        List.of(new Limb(0.07, 0.26, 0.4, 0.04, 0.025), new Limb(0.06, 0.34, 0.32, 0.6, 0.025)));
            // Dripping: a body that spreads and ripples like water, long hair down.
            case "nix" ->
                new Apparition(
                        0.1,
                        0.12,
                        0.2,
                        0.08,
                        0.35,
                        0.05,
                        26,
                        0.3,
                        0.3,
                        0.038,
                        0.022,
                        0,
                        0,
                        0,
                        0,
                        List.of(new Limb(0.08, 0.1, 0.14, 0.6, 0.02)));
            // Squat and wide, a hearth spirit.
            case "domovoi" ->
                new Apparition(
                        0.13, 0.16, 0.27, 0.15, 0.02, 0.01, 10, 0.3, 0.2, 0.04, 0.025, 0.04, 0.05, 0, 0, List.of());
            // Bent under what it carries, arms forward and wide.
            case "hoarder" ->
                new Apparition(
                        0.1,
                        0.18,
                        0.26,
                        0.12,
                        0.12,
                        0.015,
                        12,
                        0.5,
                        0.2,
                        0.034,
                        0.02,
                        0,
                        0,
                        0.12,
                        0,
                        List.of(new Limb(0.12, 0.32, 0.36, 0.48, 0.045)));
            // Horned, broad shouldered.
            case "demon" ->
                new Apparition(
                        0.1,
                        0.14,
                        0.23,
                        0.13,
                        0.08,
                        0.01,
                        8,
                        0.6,
                        0.35,
                        0.04,
                        0.022,
                        0.03,
                        0.01,
                        0,
                        0,
                        List.of(new Limb(0.06, 0.07, 0.14, -0.1, 0.022), new Limb(0.14, 0.26, 0.24, 0.55, 0.04)));
            // Thin and tilted, a grin, one arm up.
            case "trickster" ->
                new Apparition(
                        0.095,
                        0.13,
                        0.21,
                        0.06,
                        0.1,
                        0.02,
                        15,
                        0.55,
                        0.25,
                        0.036,
                        0.02,
                        0.06,
                        0.012,
                        0.1,
                        0,
                        List.of(new Limb(0.06, 0.28, 0.3, 0.1, 0.025)));
            default -> SHADE;
        };
    }

    /** Lays the figure, {@code tall} pixels high with its head's top at ({@code cx}, {@code top}), over the grey. */
    void paint(double[] grey, double cx, double top, double tall) {
        for (int y = 0; y < Picture.HEIGHT; y++) {
            for (int x = 0; x < Picture.WIDTH; x++) {
                double t = (y - top) / tall;
                double u = (x - cx) / tall - lean * t;
                if (t < -0.15 || t > 1.05 || Math.abs(u) > 0.5) {
                    continue;
                }
                double head = soft(headR - Math.hypot(u, t - headY), 0.035);
                double wave = ripple * Math.sin(t * rippleFreq + phase);
                double width = half + flare * Math.max(0, t - bodyTop - 0.01) + wave;
                double body = t < bodyTop ? 0 : soft(width - Math.abs(u), 0.06) * Math.pow(Math.max(0, 1 - t), fade);
                double arms = 0;
                for (Limb limb : limbs) {
                    arms = Math.max(arms, limb(limb, Math.abs(u), t));
                }
                double glow = soft(0.22 - Math.hypot(u * 1.4, (t - 0.45) * 0.8), 0.2) * halo;
                double eyes = soft(
                        eyeR
                                - Math.min(
                                        Math.hypot(u - eyeGap, t - headY + 0.005),
                                        Math.hypot(u + eyeGap, t - headY + 0.005)),
                        0.012);
                double mouth = mouthW <= 0 ? 0 : soft(1 - Math.hypot(u / mouthW, (t - headY - 0.045) / mouthH), 0.4);
                double mist = Math.max(Math.max(head, body), arms * 0.8) * 0.72 + glow;
                int i = x + y * Picture.WIDTH;
                grey[i] += (238 - grey[i]) * Math.min(1, mist);
                grey[i] *= 1 - 0.75 * Math.max(eyes, mouth) * head;
            }
        }
    }

    /** How much of a limb covers the point; it thins towards its tip. */
    private static double limb(Limb limb, double u, double t) {
        double dx = limb.x1 - limb.x0;
        double dt = limb.t1 - limb.t0;
        double along = Math.max(0, Math.min(1, ((u - limb.x0) * dx + (t - limb.t0) * dt) / (dx * dx + dt * dt)));
        double distance = Math.hypot(u - (limb.x0 + dx * along), t - (limb.t0 + dt * along));
        return soft(limb.w * (1 - 0.5 * along) - distance, 0.03) * (1 - 0.4 * along);
    }

    /** 0 outside, 1 inside, ramping over {@code edge} across the border of a signed distance. */
    private static double soft(double distance, double edge) {
        return Math.max(0, Math.min(1, distance / edge + 0.5));
    }
}
