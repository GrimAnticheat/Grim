package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class ProtocolLibPacketReader {
    private final LegacyAntiCheatPlugin plugin;
    private final Set<String> reflectionWarnings = java.util.Collections.synchronizedSet(new HashSet<String>());

    ProtocolLibPacketReader(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    int readIntField(Object handle, int fallbackIndex, String... preferredNames) {
        Integer value = readIntegerValue(handle, fallbackIndex, preferredNames);
        return value == null ? 0 : value.intValue();
    }

    Integer readIntegerValue(Object handle, int fallbackIndex, String... preferredNames) {
        if (handle == null) {
            return null;
        }
        Integer direct = toInteger(readFieldValue(handle, preferredNames));
        if (direct != null) {
            return direct;
        }
        int seen = 0;
        for (Field field : allFields(handle.getClass())) {
            try {
                field.setAccessible(true);
                Integer value = toInteger(field.get(handle));
                if (value == null) {
                    continue;
                }
                if (seen == fallbackIndex) {
                    return value;
                }
                seen++;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    Short readShortValue(Object handle, int fallbackIndex, String... preferredNames) {
        if (handle == null) {
            return null;
        }
        Short direct = toShort(readFieldValue(handle, preferredNames));
        if (direct != null) {
            return direct;
        }
        int seen = 0;
        for (Field field : allFields(handle.getClass())) {
            try {
                field.setAccessible(true);
                Short value = toShort(field.get(handle));
                if (value == null) {
                    continue;
                }
                if (seen == fallbackIndex) {
                    return value;
                }
                seen++;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    Double readDoubleValue(Object handle, int fallbackIndex, String... preferredNames) {
        if (handle == null) {
            return null;
        }
        Double direct = toDouble(readFieldValue(handle, preferredNames));
        if (direct != null) {
            return direct;
        }
        int seen = 0;
        for (Field field : allFields(handle.getClass())) {
            try {
                field.setAccessible(true);
                Double value = toDouble(field.get(handle));
                if (value == null) {
                    continue;
                }
                if (seen == fallbackIndex) {
                    return value;
                }
                seen++;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    Float readFloatValue(Object handle, int fallbackIndex, String... preferredNames) {
        if (handle == null) {
            return null;
        }
        Float direct = toFloat(readFieldValue(handle, preferredNames));
        if (direct != null) {
            return direct;
        }
        int seen = 0;
        for (Field field : allFields(handle.getClass())) {
            try {
                field.setAccessible(true);
                Float value = toFloat(field.get(handle));
                if (value == null) {
                    continue;
                }
                if (seen == fallbackIndex) {
                    return value;
                }
                seen++;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    Boolean readBooleanValue(Object handle, int fallbackIndex, String... preferredNames) {
        if (handle == null) {
            return null;
        }
        Object direct = readFieldValue(handle, preferredNames);
        if (direct instanceof Boolean) {
            return (Boolean) direct;
        }
        int seen = 0;
        for (Field field : allFields(handle.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(handle);
                if (!(value instanceof Boolean)) {
                    continue;
                }
                if (seen == fallbackIndex) {
                    return (Boolean) value;
                }
                seen++;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    int[] readIntArrayField(Object handle, int fallbackIndex, String... preferredNames) {
        Object direct = readFieldValue(handle, preferredNames);
        if (direct instanceof int[]) {
            return (int[]) direct;
        }
        int seen = 0;
        for (Field field : allFields(handle.getClass())) {
            try {
                field.setAccessible(true);
                if (field.getType().isArray() && field.getType().getComponentType() == int.class) {
                    int[] value = (int[]) field.get(handle);
                    if (seen == fallbackIndex) {
                        return value;
                    }
                    seen++;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    Object readFieldValue(Object handle, String... preferredNames) {
        if (handle == null || preferredNames == null) {
            return null;
        }
        for (String name : preferredNames) {
            try {
                Field field = findField(handle.getClass(), name);
                return field.get(handle);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    Object readFirstEnumField(Object handle, String... typeHints) {
        if (handle == null) {
            return null;
        }
        for (Field field : allFields(handle.getClass())) {
            try {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (!fieldType.isEnum()) {
                    continue;
                }
                if (typeHints == null || typeHints.length == 0) {
                    return field.get(handle);
                }
                String typeName = fieldType.getSimpleName().toUpperCase(Locale.ROOT);
                for (String hint : typeHints) {
                    if (typeName.contains(hint)) {
                        return field.get(handle);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    Integer resolveEntityActionId(Object actionObj) {
        if (actionObj == null) {
            return null;
        }
        if (actionObj instanceof Number) {
            return Integer.valueOf(((Number) actionObj).intValue());
        }
        String name = String.valueOf(actionObj).toUpperCase(Locale.ROOT);
        if (name.contains("START_SNEAK")) {
            return Integer.valueOf(1);
        }
        if (name.contains("STOP_SNEAK") || name.contains("RELEASE_SHIFT")) {
            return Integer.valueOf(2);
        }
        if (name.contains("STOP_SLEEP")) {
            return Integer.valueOf(3);
        }
        if (name.contains("START_SPRINT")) {
            return Integer.valueOf(4);
        }
        if (name.contains("STOP_SPRINT")) {
            return Integer.valueOf(5);
        }
        if (name.contains("RIDING_JUMP") || name.contains("HORSE_JUMP")) {
            return Integer.valueOf(6);
        }
        if (name.contains("OPEN_INVENTORY")) {
            return Integer.valueOf(7);
        }
        if (actionObj instanceof Enum<?>) {
            int ordinal = ((Enum<?>) actionObj).ordinal();
            if (ordinal >= 0 && ordinal <= 6) {
                return Integer.valueOf(ordinal + 1);
            }
        }
        return null;
    }

    Integer resolveDigAction(Object handle) {
        Object actionObj = readFieldValue(handle, "e", "status", "digType", "f");
        if (actionObj == null) {
            actionObj = readFirstEnumField(handle, "DIGTYPE", "DIGACTION");
        }
        Integer mapped = mapDigActionId(actionObj);
        if (mapped != null) {
            return mapped;
        }
        Integer numeric = readIntegerValue(handle, 4, "e", "status");
        if (numeric == null) {
            numeric = readIntegerValue(handle, 3, "d");
        }
        if (numeric != null && numeric.intValue() >= 0 && numeric.intValue() <= 5) {
            return numeric;
        }
        return null;
    }

    boolean isUseEntityAttack(Object actionObj) {
        if (actionObj == null) {
            return true;
        }
        return String.valueOf(actionObj).toUpperCase(Locale.ROOT).contains("ATTACK");
    }

    void warnReflectionFailureOnce(String packetName, String target) {
        boolean strict = plugin.getConfig().getBoolean("protocollib.strict-reflection",
                plugin.getConfig().getBoolean("netty.strict-reflection", false));
        if (!strict) {
            return;
        }
        String key = packetName + ":" + target;
        if (reflectionWarnings.add(key)) {
            plugin.getLogger().warning("[GLAC] ProtocolLib reflection failed for " + packetName
                    + " field(s) " + target + ", degraded packet parsing.");
        }
    }

    private Integer mapDigActionId(Object actionObj) {
        if (actionObj == null) {
            return null;
        }
        if (actionObj instanceof Number) {
            return Integer.valueOf(((Number) actionObj).intValue());
        }
        String name = String.valueOf(actionObj).toUpperCase(Locale.ROOT);
        if (name.contains("START_DESTROY")) {
            return Integer.valueOf(0);
        }
        if (name.contains("ABORT_DESTROY")) {
            return Integer.valueOf(1);
        }
        if (name.contains("STOP_DESTROY")) {
            return Integer.valueOf(2);
        }
        if (name.contains("DROP_ALL")) {
            return Integer.valueOf(3);
        }
        if (name.contains("DROP_ITEM")) {
            return Integer.valueOf(4);
        }
        if (name.contains("RELEASE_USE")) {
            return Integer.valueOf(5);
        }
        if (actionObj instanceof Enum<?>) {
            int ordinal = ((Enum<?>) actionObj).ordinal();
            if (ordinal >= 0 && ordinal <= 5) {
                return Integer.valueOf(ordinal);
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Short) {
            return Integer.valueOf(((Short) value).intValue());
        }
        if (value instanceof Byte) {
            return Integer.valueOf(((Byte) value).intValue());
        }
        if (value instanceof Long) {
            return Integer.valueOf(((Long) value).intValue());
        }
        return null;
    }

    private Short toShort(Object value) {
        if (value instanceof Short) {
            return (Short) value;
        }
        if (value instanceof Integer) {
            return Short.valueOf(((Integer) value).shortValue());
        }
        if (value instanceof Byte) {
            return Short.valueOf(((Byte) value).shortValue());
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Float) {
            return Double.valueOf(((Float) value).doubleValue());
        }
        return null;
    }

    private Float toFloat(Object value) {
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Double) {
            return Float.valueOf(((Double) value).floatValue());
        }
        return null;
    }

    private Field findField(Class<?> type, String name) throws Exception {
        Class<?> search = type;
        while (search != null) {
            try {
                Field field = search.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                search = search.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private java.util.List<Field> allFields(Class<?> type) {
        java.util.List<Field> fields = new java.util.ArrayList<Field>();
        Class<?> search = type;
        while (search != null) {
            Field[] declared = search.getDeclaredFields();
            for (Field field : declared) {
                fields.add(field);
            }
            search = search.getSuperclass();
        }
        return fields;
    }
}
