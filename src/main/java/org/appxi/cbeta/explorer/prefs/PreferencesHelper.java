package org.appxi.cbeta.explorer.prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PreferencesHelper {
    private static final List<PreferencesConfigurable> configurables = new ArrayList<>();

    private PreferencesHelper() {
    }

    public static void addConfigurable(PreferencesConfigurable configurable) {
        if (!configurables.contains(configurable))
            configurables.add(configurable);
    }

    public static void removeConfigurable(PreferencesConfigurable configurable) {
        configurables.remove(configurable);
    }

    public static List<PreferencesConfigurable> getConfigurables() {
        return Collections.unmodifiableList(configurables);
    }
}
