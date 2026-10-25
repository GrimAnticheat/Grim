package ac.grim.grimac.manager.datastore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Classifies expected closed-connection failures while ownership workers stop. */
final class ShutdownDiagnostics {
    private ShutdownDiagnostics() {}

    static boolean isClosedConnectionFailure(@Nullable Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 32) {
            String className = current.getClass().getName().toLowerCase(Locale.ROOT);
            String message = current.getMessage();
            String lowerMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);
            if (className.contains("closedconnection")
                    || lowerMessage.contains("connection is closed")
                    || lowerMessage.contains("connection has been closed")
                    || lowerMessage.contains("pool has been closed")
                    || lowerMessage.contains("pool is closed")
                    || (lowerMessage.contains("hikaripool")
                    && lowerMessage.contains("closed"))) {
                return true;
            }
            Throwable next = current.getCause();
            if (next == null || next == current) return false;
            current = next;
        }
        return false;
    }

    static @NotNull String rootMessage(@Nullable Throwable throwable) {
        Throwable current = throwable;
        Throwable last = throwable;
        int depth = 0;
        while (current != null && depth++ < 32) {
            last = current;
            Throwable next = current.getCause();
            if (next == null || next == current) break;
            current = next;
        }
        if (last == null) return "unknown";
        String message = last.getMessage();
        return message == null || message.isBlank()
                ? last.getClass().getSimpleName()
                : message;
    }

}
