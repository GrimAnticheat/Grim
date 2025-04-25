package ac.grim.grimac.utils.math;

// My previous rant was wrong, we have 4 fastmath versions.  what the fuck optifine.
public class LegacyFastMath {
    private static final float[] SIN_TABLE_FAST = new float[4096];

    static {
        for (int i = 0; i < 4096; ++i) {
            SIN_TABLE_FAST[i] = (float) Math.sin(((float) i + 0.5f) / 4096f * ((float) Math.PI * 2f));
        }

        for (int i = 0; i < 360; i += 90) {
            SIN_TABLE_FAST[(int) ((float) i * 11.377778f) & 4095] = (float) Math.sin(GrimMath.radians((float) i));
        }
    }

    public static float sin(float par0) {
        return SIN_TABLE_FAST[(int) (par0 * 651.8986f) & 4095];
    }

    public static float cos(float par0) {
        return SIN_TABLE_FAST[(int) ((par0 + ((float) Math.PI / 2f)) * 651.8986f) & 4095];
    }
}
