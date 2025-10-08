package ac.grim.grimac.events;

import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.GrimEvent;
import ac.grim.grimac.api.event.GrimEventHandler;
import ac.grim.grimac.api.event.GrimEventListener;
import ac.grim.grimac.api.event.events.*;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class OptimizedEventBus implements EventBus {
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final GrimExtensionManager extensionManager;
    // Store all listeners together by event type
    private final Map<Class<? extends GrimEvent>, AtomicReference<OptimizedListener[]>> listenerMap = new ConcurrentHashMap<>();

    public OptimizedEventBus(GrimExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
        prefillKnownEventTypes(listenerMap);
    }

    /**
     * Minor optimization to prefill the listenerMap for known existing events so that they don't have to be computed later
     * When registering an event of the type for the first time
     * @param map
     */
    private void prefillKnownEventTypes(Map<Class<? extends GrimEvent>, AtomicReference<OptimizedListener[]>> map) {
        // Add all your known event types here
        map.put(GrimReloadEvent.class, new AtomicReference<>(new OptimizedListener[0]));
        map.put(GrimQuitEvent.class, new AtomicReference<>(new OptimizedListener[0]));
        map.put(GrimJoinEvent.class, new AtomicReference<>(new OptimizedListener[0]));

        map.put(FlagEvent.class, new AtomicReference<>(new OptimizedListener[0]));
        map.put(CommandExecuteEvent.class, new AtomicReference<>(new OptimizedListener[0]));
        map.put(CompletePredictionEvent.class, new AtomicReference<>(new OptimizedListener[0]));

        // Base event type
        map.put(GrimEvent.class, new AtomicReference<>(new OptimizedListener[0]));
    }


    @Override
    public void registerAnnotatedListeners(@NotNull Object pluginContext, @NotNull Object listener) {
        GrimPlugin plugin = extensionManager.getPlugin(pluginContext);
        registerAnnotatedListeners(plugin, listener);
    }

    @Override
    public void registerAnnotatedListeners(GrimPlugin plugin, @NotNull Object listener) {
        registerMethods(plugin, listener, listener.getClass());
    }

    @Override
    public void registerStaticAnnotatedListeners(@NotNull Object pluginContext, @NotNull Class<?> clazz) {
        GrimPlugin plugin = extensionManager.getPlugin(pluginContext);
        registerStaticAnnotatedListeners(plugin, clazz);
    }

    @Override
    public void registerStaticAnnotatedListeners(GrimPlugin plugin, @NotNull Class<?> clazz) {
        registerMethods(plugin, null, clazz);
    }

    @Override
    public void unregisterListeners(@NotNull Object pluginContext, @NotNull Object listener) {
        GrimPlugin plugin = extensionManager.getPlugin(pluginContext);
        unregisterListeners(plugin, listener);
    }

    private void registerMethods(GrimPlugin plugin, @Nullable Object instance, @NotNull Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            GrimEventHandler annotation = method.getAnnotation(GrimEventHandler.class);
            if (annotation != null && method.getParameterCount() == 1) {
                Class<?> eventType = method.getParameterTypes()[0];
                if (GrimEvent.class.isAssignableFrom(eventType)) {
                    try {
                        if (instance == null && !Modifier.isStatic(method.getModifiers())) {
                            continue;
                        }

                        method.setAccessible(true);
                        MethodHandle handle = lookup.unreflect(method);

                        final GrimEventListener<GrimEvent> listener = getGrimEventListener(instance, handle, eventType);

                        OptimizedListener optimizedListener = new OptimizedListener(
                                plugin, listener, annotation.priority(),
                                annotation.ignoreCancelled(), method.getDeclaringClass(), instance
                        );

                        addListener((Class<? extends GrimEvent>) eventType, optimizedListener);
                    } catch (IllegalAccessException e) {
                        LogUtil.error("Failed to register listener for " + eventType.getName(), e);
                    }
                }
            }
        }
    }

    // Decide which listener to create at registration time to avoid the runtime 'if' check.
    private @NotNull GrimEventListener<GrimEvent> getGrimEventListener(@Nullable Object instance, MethodHandle handle, Class<?> eventType) {
        final GrimEventListener<GrimEvent> listener;
        if (instance != null) {
            // For instance methods, create a listener that captures the instance.
            listener = event -> {
                try {
                    handle.invoke(instance, event);
                } catch (Throwable throwable) {
                    throw new RuntimeException("Failed to invoke listener for " + eventType.getName(), throwable);
                }
            };
        } else {
            // For static methods, create a listener that does NOT capture an instance.
            // This is slightly more memory efficient and avoids the null check.
            listener = event -> {
                try {
                    handle.invoke(event);
                } catch (Throwable throwable) {
                    throw new RuntimeException("Failed to invoke listener for " + eventType.getName(), throwable);
                }
            };
        }
        return listener;
    }

    private void addListener(Class<? extends GrimEvent> eventType, OptimizedListener newListener) {
        AtomicReference<OptimizedListener[]> ref = listenerMap.computeIfAbsent(
                eventType, k -> new AtomicReference<>(new OptimizedListener[0])
        );

        while (true) {
            OptimizedListener[] oldArray = ref.get();

            // Find insertion point using Arrays.binarySearch
            // We use a comparator for descending order (higher priorities first)
            int insertionPoint = Arrays.binarySearch(oldArray, newListener,
                    (a, b) -> Integer.compare(b.priority, a.priority));

            // If negative, convert to insertion point
            if (insertionPoint < 0) {
                insertionPoint = -(insertionPoint + 1);
            } else {
                // If we found an exact match, insert after the last equal priority element
                // to maintain stable ordering
                while (insertionPoint < oldArray.length - 1 &&
                        oldArray[insertionPoint + 1].priority == newListener.priority) {
                    insertionPoint++;
                }
                insertionPoint++; // Insert after the last equal priority element
            }

            OptimizedListener[] newArray = new OptimizedListener[oldArray.length + 1];

            // Copy elements before insertion point
            System.arraycopy(oldArray, 0, newArray, 0, insertionPoint);

            // Insert the new listener
            newArray[insertionPoint] = newListener;

            // Copy remaining elements, shifted by one
            System.arraycopy(oldArray, insertionPoint, newArray, insertionPoint + 1,
                    oldArray.length - insertionPoint);

            if (ref.compareAndSet(oldArray, newArray)) {
                break;
            }
            // If CAS failed, loop and try again
        }
    }

    @Override
    public void unregisterListeners(GrimPlugin plugin, Object instance) {
        for (Map.Entry<Class<? extends GrimEvent>, AtomicReference<OptimizedListener[]>> entry : listenerMap.entrySet()) {
            removeListeners(entry.getKey(), entry.getValue(),
                    listener -> listener.plugin.equals(plugin) &&
                            listener.instance != null &&
                            listener.instance.equals(instance));
        }
    }

    @Override
    public void unregisterStaticListeners(@NotNull Object pluginContext, @NotNull Class<?> clazz) {
        GrimPlugin plugin = extensionManager.getPlugin(pluginContext);
        unregisterStaticListeners(plugin, clazz);
    }

    @Override
    public void unregisterStaticListeners(GrimPlugin plugin, Class<?> clazz) {
        for (Map.Entry<Class<? extends GrimEvent>, AtomicReference<OptimizedListener[]>> entry : listenerMap.entrySet()) {
            removeListeners(entry.getKey(), entry.getValue(),
                    listener -> listener.plugin.equals(plugin) &&
                            listener.instance == null &&
                            listener.declaringClass.equals(clazz));
        }
    }

    @Override public void unregisterAllListeners(@NotNull Object pluginContext) {
        GrimPlugin plugin = extensionManager.getPlugin(pluginContext);
        unregisterAllListeners(plugin);
    }

    @Override
    public void unregisterAllListeners(GrimPlugin plugin) {
        for (Map.Entry<Class<? extends GrimEvent>, AtomicReference<OptimizedListener[]>> entry : listenerMap.entrySet()) {
            removeListeners(entry.getKey(), entry.getValue(),
                    listener -> listener.plugin.equals(plugin));
        }
    }

    @Override
    public void unregisterListener(@NotNull Object pluginContext, @NotNull GrimEventListener<?> listener) {
        GrimPlugin plugin = extensionManager.getPlugin(pluginContext);
        unregisterListener(plugin, listener);
    }

    @Override
    public void unregisterListener(GrimPlugin plugin, GrimEventListener<?> eventListener) {
        for (Map.Entry<Class<? extends GrimEvent>, AtomicReference<OptimizedListener[]>> entry : listenerMap.entrySet()) {
            removeListeners(entry.getKey(), entry.getValue(),
                    listener -> listener.plugin.equals(plugin) &&
                            listener.listener.equals(eventListener));
        }
    }

    private void removeListeners(Class<? extends GrimEvent> eventType,
                                 AtomicReference<OptimizedListener[]> ref,
                                 Predicate<OptimizedListener> filter) {
        while (true) {
            OptimizedListener[] oldArray = ref.get();

            // Count how many listeners will remain after filtering
            int remaining = 0;
            for (OptimizedListener listener : oldArray) {
                if (!filter.test(listener)) {
                    remaining++;
                }
            }

            // If no change, return early
            if (remaining == oldArray.length) {
                return;
            }

            // Create new array with filtered listeners
            OptimizedListener[] newArray = new OptimizedListener[remaining];
            int index = 0;
            for (OptimizedListener listener : oldArray) {
                if (!filter.test(listener)) {
                    newArray[index++] = listener;
                }
            }

            // Try to update the array
            if (ref.compareAndSet(oldArray, newArray)) {
                // If array is empty, remove the event type entry
                if (newArray.length == 0) {
                    listenerMap.remove(eventType);
                }
                break;
            }
            // If CAS failed, retry
        }
    }

    @Override
    public void post(@NotNull GrimEvent event) {
        Class<?> currentEventType = event.getClass();
        while (GrimEvent.class.isAssignableFrom(currentEventType)) {
            AtomicReference<OptimizedListener[]> ref = listenerMap.get(currentEventType);
            if (ref != null) {
                // Get current array - no locking needed for reads
                OptimizedListener[] listeners = ref.get();
                for (OptimizedListener listener : listeners) {
                    try {
                        if (event.isCancelled() && !listener.ignoreCancelled) {
                            continue;
                        }
                        listener.listener.handle(event);
                    } catch (Throwable throwable) {
                        LogUtil.error("Error handling event " + event.getEventName(), throwable);
                    }
                }
            }
            currentEventType = currentEventType.getSuperclass();
        }
    }

    @Override
    public <T extends GrimEvent> void subscribe(@NotNull Object pluginContext, @NotNull Class<T> eventType, @NotNull GrimEventListener<T> listener, int priority, boolean ignoreCancelled, @NotNull Class<?> declaringClass) {

    }

    @Override
    public <T extends GrimEvent> void subscribe(GrimPlugin plugin, @NotNull Class<T> eventType,
                                                @NotNull GrimEventListener<T> listener, int priority,
                                                boolean ignoreCancelled, @NotNull Class<?> declaringClass) {
        @SuppressWarnings("unchecked")
        OptimizedListener optimizedListener = new OptimizedListener(
                plugin, (GrimEventListener<GrimEvent>)listener, priority,
                ignoreCancelled, declaringClass, null
        );

        addListener(eventType, optimizedListener);
    }

    private static class OptimizedListener {
        final GrimPlugin plugin;
        final GrimEventListener<GrimEvent> listener;
        final int priority;
        final boolean ignoreCancelled;
        final Class<?> declaringClass;
        final Object instance;

        OptimizedListener(GrimPlugin plugin, GrimEventListener<GrimEvent> listener,
                          int priority, boolean ignoreCancelled,
                          Class<?> declaringClass, Object instance) {
            this.plugin = plugin;
            this.listener = listener;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            this.declaringClass = declaringClass;
            this.instance = instance;
        }
    }
}
