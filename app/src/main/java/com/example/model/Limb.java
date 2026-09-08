package com.example.model;

import android.content.Context;
import com.example.R;

public enum Limb {
    RIGHT_ARM(0, R.string.limb_right_arm, "Sağ Kol", R.drawable.ic_arm),
    RIGHT_LEG(1, R.string.limb_right_leg, "Sağ Bacak", R.drawable.ic_leg),
    LEFT_ARM(2, R.string.limb_left_arm, "Sol Kol", R.drawable.ic_arm),
    LEFT_LEG(3, R.string.limb_left_leg, "Sol Bacak", R.drawable.ic_leg);

    private final int index;
    private final int stringResId;
    private final String defaultName;
    private final int iconResId;

    Limb(int index, int stringResId, String defaultName, int iconResId) {
        this.index = index;
        this.stringResId = stringResId;
        this.defaultName = defaultName;
        this.iconResId = iconResId;
    }

    public int getIndex() {
        return index;
    }

    public int getStringResId() {
        return stringResId;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getLocalizedName(Context context) {
        if (context != null) {
            return context.getString(stringResId);
        }
        return defaultName;
    }

    public static Limb fromIndex(int index) {
        int normalized = ((index % 4) + 4) % 4;
        for (Limb limb : values()) {
            if (limb.index == normalized) {
                return limb;
            }
        }
        return RIGHT_ARM;
    }
}
