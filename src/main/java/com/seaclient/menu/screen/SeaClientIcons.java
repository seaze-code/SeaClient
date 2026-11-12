package com.seaclient.menu.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.function.BiFunction;

/**
 * Generates high-resolution (64x64) anti-aliased Signed Distance Field (SDF) icons
 * matching Lucide/Feather vector icons from the reference UI design.
 */
public final class SeaClientIcons {

    public static final Identifier USER = Identifier.fromNamespaceAndPath("seaclient", "icon_user");
    public static final Identifier SINGLEPLAYER = Identifier.fromNamespaceAndPath("seaclient", "icon_singleplayer");
    public static final Identifier MULTIPLAYER = Identifier.fromNamespaceAndPath("seaclient", "icon_multiplayer");
    public static final Identifier REALMS = Identifier.fromNamespaceAndPath("seaclient", "icon_realms");
    public static final Identifier SETTINGS = Identifier.fromNamespaceAndPath("seaclient", "icon_settings");
    public static final Identifier SPARKLES = Identifier.fromNamespaceAndPath("seaclient", "icon_sparkles");
    public static final Identifier SWORD = Identifier.fromNamespaceAndPath("seaclient", "icon_sword");
    public static final Identifier FOLDER = Identifier.fromNamespaceAndPath("seaclient", "icon_folder");
    public static final Identifier GLOBE = Identifier.fromNamespaceAndPath("seaclient", "icon_globe");
    public static final Identifier POWER = Identifier.fromNamespaceAndPath("seaclient", "icon_power");
    public static final Identifier PREV = Identifier.fromNamespaceAndPath("seaclient", "icon_prev");
    public static final Identifier PLAY = Identifier.fromNamespaceAndPath("seaclient", "icon_play");
    public static final Identifier PAUSE = Identifier.fromNamespaceAndPath("seaclient", "icon_pause");
    public static final Identifier NEXT = Identifier.fromNamespaceAndPath("seaclient", "icon_next");
    public static final Identifier HOME = Identifier.fromNamespaceAndPath("seaclient", "icon_home");
    public static final Identifier EYE = Identifier.fromNamespaceAndPath("seaclient", "icon_eye");
    public static final Identifier BOLT = Identifier.fromNamespaceAndPath("seaclient", "icon_bolt");
    public static final Identifier VOLUME_MUTE = Identifier.fromNamespaceAndPath("seaclient", "icon_volume_mute");
    public static final Identifier BURST = Identifier.fromNamespaceAndPath("seaclient", "icon_burst");
    public static final Identifier SLIDERS = Identifier.fromNamespaceAndPath("seaclient", "icon_sliders");
    public static final Identifier CPU = Identifier.fromNamespaceAndPath("seaclient", "icon_cpu");

    private static boolean registered = false;

    private SeaClientIcons() {
    }

    public static void ensureRegistered(Minecraft mc) {
        if (registered) {
            return;
        }
        registered = true;

        registerSdfIcon(mc, USER, (x, y) -> {
            // Head circle + shoulders arch
            double dHead = sdCircle(x, y, 32.0, 21.0, 8.5);
            double dShoulders = Math.max(
                    Math.abs(sdCircle(x, y, 32.0, 52.0, 16.5)) - 3.2,
                    y - 49.0
            );
            double dBodyFill = Math.max(sdCircle(x, y, 32.0, 51.0, 16.0), y - 48.0);
            return Math.min(dHead, Math.min(dShoulders, dBodyFill));
        });

        registerSdfIcon(mc, SINGLEPLAYER, (x, y) -> sdTriangle(x, y, 18.0, 12.0, 50.0, 32.0, 18.0, 52.0));

        registerSdfIcon(mc, MULTIPLAYER, (x, y) -> {
            // Two stacked server rack boxes (outlined) with status dots
            double box1 = Math.abs(sdBox(x, y, 32.0, 20.5, 20.0, 6.5)) - 2.2;
            double box2 = Math.abs(sdBox(x, y, 32.0, 43.5, 20.0, 6.5)) - 2.2;
            double dot1 = sdCircle(x, y, 20.5, 20.5, 2.3);
            double dot2 = sdCircle(x, y, 20.5, 43.5, 2.3);
            return Math.min(Math.min(box1, box2), Math.min(dot1, dot2));
        });

        registerSdfIcon(mc, REALMS, (x, y) -> {
            // Cloud union of circles + bottom box, outlined
            double c1 = sdCircle(x, y, 23.0, 36.0, 9.5);
            double c2 = sdCircle(x, y, 33.0, 27.0, 12.5);
            double c3 = sdCircle(x, y, 43.5, 36.5, 9.0);
            double base = sdBox(x, y, 33.0, 39.5, 11.0, 6.0);
            double union = Math.min(Math.min(c1, c2), Math.min(c3, base));
            return Math.abs(union) - 2.3;
        });

        registerSdfIcon(mc, SETTINGS, (x, y) -> {
            // 8-tooth Lucide cog/gear outline
            double dx = x - 32.0;
            double dy = y - 32.0;
            double r = Math.sqrt(dx * dx + dy * dy);
            double angle = Math.atan2(dy, dx);
            double teethWave = Math.cos(angle * 8.0);
            double outerR = 18.5 + 4.2 * Math.clamp(teethWave * 2.2, -1.0, 1.0);
            double gearBody = Math.max(r - outerR, 11.2 - r);
            double innerHole = Math.abs(r - 9.0) - 2.2;
            return Math.min(gearBody, innerHole);
        });

        registerSdfIcon(mc, SPARKLES, (x, y) -> {
            // Main 4-point concave sparkle star + secondary top-right star + bottom-left dot
            double mainStar = sdFourPointStar(x - 28.0, y - 35.0, 18.0, 0.56);
            double topStar = sdFourPointStar(x - 47.0, y - 17.0, 9.5, 0.58);
            double smallStar = sdFourPointStar(x - 16.0, y - 18.0, 6.5, 0.60);
            return Math.min(mainStar, Math.min(topStar, smallStar));
        });

        registerSdfIcon(mc, SWORD, (x, y) -> {
            // Diagonal Lucide sword: blade, crossguard, grip, pommel
            double blade = sdSegment(x, y, 23.0, 41.0, 50.0, 14.0) - 3.2;
            double guard = sdSegment(x, y, 16.0, 32.0, 32.0, 48.0) - 2.5;
            double grip = sdSegment(x, y, 14.0, 50.0, 23.0, 41.0) - 2.4;
            double pommel = sdCircle(x, y, 12.5, 51.5, 3.2);
            return Math.min(Math.min(blade, guard), Math.min(grip, pommel));
        });

        registerSdfIcon(mc, FOLDER, (x, y) -> {
            // Outlined folder with top-left tab
            double mainBox = sdBox(x, y, 32.0, 35.0, 21.0, 13.0);
            double tabBox = sdBox(x, y, 20.0, 20.5, 9.0, 4.0);
            double union = Math.min(mainBox, tabBox);
            return Math.abs(union) - 2.3;
        });

        registerSdfIcon(mc, GLOBE, (x, y) -> {
            // Wireframe globe: outer circle + meridian ellipse + equator + latitude lines
            double outer = Math.abs(sdCircle(x, y, 32.0, 32.0, 20.5)) - 2.1;
            double dx = (x - 32.0) / 10.5;
            double dy = (y - 32.0) / 20.5;
            double meridianDist = Math.abs(Math.sqrt(dx * dx + dy * dy) - 1.0) * 10.5 - 1.9;
            double equator = Math.abs(y - 32.0) - 1.9;
            double insideGlobe = sdCircle(x, y, 32.0, 32.0, 21.0);
            double innerLines = Math.max(Math.min(meridianDist, equator), insideGlobe);
            return Math.min(outer, innerLines);
        });

        registerSdfIcon(mc, POWER, (x, y) -> {
            // Standby power icon: open circle arc + top vertical bar
            double dx = x - 32.0;
            double dy = y - 34.0;
            double r = Math.sqrt(dx * dx + dy * dy);
            double ring = Math.abs(r - 17.5) - 2.4;
            // Cut a gap at the top of the ring
            double gapCut = (dy < -9.0 && Math.abs(dx) < 8.5) ? (8.5 - Math.abs(dx)) : -100.0;
            double openRing = Math.max(ring, gapCut);
            double bar = sdSegment(x, y, 32.0, 11.0, 32.0, 31.0) - 2.4;
            return Math.min(openRing, bar);
        });

        registerSdfIcon(mc, PREV, (x, y) -> {
            double bar = sdSegment(x, y, 16.0, 16.0, 16.0, 48.0) - 2.6;
            double tri = sdTriangle(x, y, 48.0, 15.0, 22.0, 32.0, 48.0, 49.0);
            return Math.min(bar, tri);
        });

        registerSdfIcon(mc, PLAY, (x, y) -> sdTriangle(x, y, 18.0, 13.0, 50.0, 32.0, 18.0, 51.0));

        registerSdfIcon(mc, PAUSE, (x, y) -> {
            double b1 = sdBox(x, y, 22.0, 32.0, 4.2, 18.0);
            double b2 = sdBox(x, y, 42.0, 32.0, 4.2, 18.0);
            return Math.min(b1, b2);
        });

        registerSdfIcon(mc, NEXT, (x, y) -> {
            double bar = sdSegment(x, y, 48.0, 16.0, 48.0, 48.0) - 2.6;
            double tri = sdTriangle(x, y, 16.0, 15.0, 42.0, 32.0, 16.0, 49.0);
            return Math.min(bar, tri);
        });

        registerSdfIcon(mc, HOME, (x, y) -> {
            double roofL = sdSegment(x, y, 12.0, 30.0, 32.0, 13.0) - 2.4;
            double roofR = sdSegment(x, y, 32.0, 13.0, 52.0, 30.0) - 2.4;
            double wallL = sdSegment(x, y, 17.0, 27.0, 17.0, 50.0) - 2.3;
            double wallR = sdSegment(x, y, 47.0, 27.0, 47.0, 50.0) - 2.3;
            double floor = sdSegment(x, y, 17.0, 50.0, 47.0, 50.0) - 2.3;
            double door = Math.abs(sdBox(x, y, 32.0, 42.0, 5.5, 8.0)) - 2.0;
            return Math.min(Math.min(Math.min(roofL, roofR), Math.min(wallL, wallR)), Math.min(floor, door));
        });

        registerSdfIcon(mc, EYE, (x, y) -> {
            double dx = (x - 32.0) / 22.0;
            double dy = (y - 32.0) / 13.5;
            double almond = Math.abs(Math.sqrt(dx * dx + dy * dy) - 1.0) * 14.0 - 2.2;
            double pupil = Math.abs(sdCircle(x, y, 32.0, 32.0, 6.2)) - 2.2;
            return Math.min(almond, pupil);
        });

        registerSdfIcon(mc, BOLT, (x, y) -> {
            double t1 = sdTriangle(x, y, 35.0, 10.0, 15.0, 35.0, 32.0, 35.0);
            double t2 = sdTriangle(x, y, 29.0, 54.0, 49.0, 29.0, 32.0, 29.0);
            return Math.min(t1, t2);
        });

        registerSdfIcon(mc, VOLUME_MUTE, (x, y) -> {
            double box = sdBox(x, y, 16.0, 32.0, 4.5, 6.5);
            double cone = sdTriangle(x, y, 16.0, 25.0, 31.0, 15.0, 31.0, 49.0);
            double cone2 = sdTriangle(x, y, 16.0, 25.0, 16.0, 39.0, 31.0, 49.0);
            double speaker = Math.abs(Math.min(box, Math.min(cone, cone2))) - 2.1;
            double x1 = sdSegment(x, y, 39.0, 25.0, 51.0, 39.0) - 2.3;
            double x2 = sdSegment(x, y, 51.0, 25.0, 39.0, 39.0) - 2.3;
            return Math.min(speaker, Math.min(x1, x2));
        });

        registerSdfIcon(mc, BURST, (x, y) -> {
            double d = 100.0;
            for (int i = 0; i < 8; i++) {
                double ang = i * (Math.PI / 4.0);
                double r1 = 8.0;
                double r2 = (i % 2 == 0) ? 21.0 : 15.5;
                double seg = sdSegment(x, y,
                        32.0 + Math.cos(ang) * r1, 32.0 + Math.sin(ang) * r1,
                        32.0 + Math.cos(ang) * r2, 32.0 + Math.sin(ang) * r2) - 2.1;
                d = Math.min(d, seg);
            }
            return d;
        });

        registerSdfIcon(mc, SLIDERS, (x, y) -> {
            double l1 = sdSegment(x, y, 13.0, 19.0, 51.0, 19.0) - 2.0;
            double l2 = sdSegment(x, y, 13.0, 32.0, 51.0, 32.0) - 2.0;
            double l3 = sdSegment(x, y, 13.0, 45.0, 51.0, 45.0) - 2.0;
            double k1 = sdBox(x, y, 25.0, 19.0, 3.5, 5.0);
            double k2 = sdBox(x, y, 40.0, 32.0, 3.5, 5.0);
            double k3 = sdBox(x, y, 22.0, 45.0, 3.5, 5.0);
            return Math.min(Math.min(l1, Math.min(l2, l3)), Math.min(k1, Math.min(k2, k3)));
        });

        registerSdfIcon(mc, CPU, (x, y) -> {
            double outer = Math.abs(sdBox(x, y, 32.0, 32.0, 13.5, 13.5)) - 2.2;
            double inner = sdBox(x, y, 32.0, 32.0, 6.5, 6.5);
            double pins = 100.0;
            double[] pos = {24.0, 32.0, 40.0};
            for (double p : pos) {
                pins = Math.min(pins, sdSegment(x, y, p, 11.0, p, 17.0) - 2.0);
                pins = Math.min(pins, sdSegment(x, y, p, 47.0, p, 53.0) - 2.0);
                pins = Math.min(pins, sdSegment(x, y, 11.0, p, 17.0, p) - 2.0);
                pins = Math.min(pins, sdSegment(x, y, 47.0, p, 53.0, p) - 2.0);
            }
            return Math.min(Math.min(outer, inner), pins);
        });
    }

    private static void registerSdfIcon(Minecraft mc, Identifier id, BiFunction<Double, Double, Double> sdf) {
        int size = 64;
        NativeImage img = new NativeImage(size, size, true);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double d = sdf.apply(px + 0.5, py + 0.5);
                // Smooth anti-aliased edge over 1.3 pixels
                double alphaNorm = Math.clamp(0.5 - d / 1.35, 0.0, 1.0);
                int a = (int) Math.round(alphaNorm * 255.0);
                int argb = (a << 24) | 0x00FFFFFF;
                img.setPixel(px, py, argb);
            }
        }
        DynamicTexture tex = new DynamicTexture(() -> id.toString(), img);
        mc.getTextureManager().register(id, tex);
    }

    private static double sdCircle(double x, double y, double cx, double cy, double r) {
        double dx = x - cx;
        double dy = y - cy;
        return Math.sqrt(dx * dx + dy * dy) - r;
    }

    private static double sdBox(double x, double y, double cx, double cy, double hx, double hy) {
        double dx = Math.abs(x - cx) - hx;
        double dy = Math.abs(y - cy) - hy;
        double outX = Math.max(dx, 0.0);
        double outY = Math.max(dy, 0.0);
        return Math.sqrt(outX * outX + outY * outY) + Math.min(Math.max(dx, dy), 0.0);
    }

    private static double sdSegment(double px, double py, double ax, double ay, double bx, double by) {
        double pax = px - ax, pay = py - ay;
        double bax = bx - ax, bay = by - ay;
        double h = Math.clamp((pax * bax + pay * bay) / (bax * bax + bay * bay), 0.0, 1.0);
        double dx = pax - bax * h;
        double dy = pay - bay * h;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double sdFourPointStar(double dx, double dy, double radius, double exponent) {
        double ax = Math.abs(dx) / radius;
        double ay = Math.abs(dy) / radius;
        if (ax > 1.2 || ay > 1.2) {
            return 10.0;
        }
        double val = Math.pow(ax, exponent) + Math.pow(ay, exponent);
        return (val - 1.0) * radius * 0.65;
    }

    private static double sdTriangle(double px, double py, double x0, double y0, double x1, double y1, double x2, double y2) {
        double e0x = x1 - x0, e0y = y1 - y0;
        double e1x = x2 - x1, e1y = y2 - y1;
        double e2x = x0 - x2, e2y = y0 - y2;

        double v0x = px - x0, v0y = py - y0;
        double v1x = px - x1, v1y = py - y1;
        double v2x = px - x2, v2y = py - y2;

        double h0 = Math.clamp((v0x * e0x + v0y * e0y) / (e0x * e0x + e0y * e0y), 0.0, 1.0);
        double h1 = Math.clamp((v1x * e1x + v1y * e1y) / (e1x * e1x + e1y * e1y), 0.0, 1.0);
        double h2 = Math.clamp((v2x * e2x + v2y * e2y) / (e2x * e2x + e2y * e2y), 0.0, 1.0);

        double pq0x = v0x - e0x * h0, pq0y = v0y - e0y * h0;
        double pq1x = v1x - e1x * h1, pq1y = v1y - e1y * h1;
        double pq2x = v2x - e2x * h2, pq2y = v2y - e2y * h2;

        double s = Math.signum(e0x * e2y - e0y * e2x);
        double d0 = pq0x * pq0x + pq0y * pq0y;
        double d1 = pq1x * pq1x + pq1y * pq1y;
        double d2 = pq2x * pq2x + pq2y * pq2y;
        double dMin = Math.min(d0, Math.min(d1, d2));

        double c0 = s * (v0x * e0y - v0y * e0x);
        double c1 = s * (v1x * e1y - v1y * e1x);
        double c2 = s * (v2x * e2y - v2y * e2x);
        double cMin = Math.min(c0, Math.min(c1, c2));

        return -Math.sqrt(dMin) * Math.signum(cMin);
    }
}
