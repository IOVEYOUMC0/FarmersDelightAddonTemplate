package com.example.fdaddon;

/**
 * Runtime probe for the FarmersDelight surface this template compiles against.
 *
 * The template is copied by third parties and may be installed next to an older FarmersDelight build
 * that predates api.compat.ce. A direct reference to ContentRegistration in onLoad would
 * throw NoClassDefFoundError before any onEnable gate could run, so the probe uses reflection and
 * is the first thing both lifecycle callbacks execute.
 */
final class TemplateApiProbe {

    private TemplateApiProbe() {
    }

    static boolean supportsCompatCe() {
        try {
            Class<?> apiType = Class.forName("com.huidu.farmersdelight.api.FarmersDelightApi");
            Object api = apiType.getMethod("get").invoke(null);
            if (api == null) {
                return false;
            }
            int revision = (Integer) apiType.getMethod("apiVersion").invoke(api);
            boolean compatCe = (Boolean) apiType.getMethod("hasFeature", String.class)
                    .invoke(api, "compat-ce");
            return revision >= 4 && compatCe;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            // Older FD: apiVersion does not exist yet, or the class is absent entirely.
            return false;
        }
    }
}
