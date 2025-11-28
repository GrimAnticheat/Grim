package ac.grim.grimac.checks.debug;

import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;

import java.lang.management.ManagementFactory;

public class GrimDebugSettings {

    public static boolean shouldEnable() {
        // 1. If the argument is disabled, never enable debug mode.
        if (!CommonGrimArguments.DEBUG_BREAKPOINT_MODE.value()) {
            return false;
        }

        // 2. Check for Debugger (Startup Agent OR Dynamic Attach)
        return isDebuggerAttached();
    }

    private static boolean isDebuggerAttached() {
        // Fast Path: Check startup arguments for -agentlib:jdwp
        if (ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp")) {
            return true;
        }

        // Slow Path (Dynamic Attach): Scan threads for JDWP/Signal Dispatcher
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                String name = t.getName();
                if (name.contains("JDWP")) return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }
}
