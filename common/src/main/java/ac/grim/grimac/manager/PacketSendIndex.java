package ac.grim.grimac.manager;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

// sendTypes() -> typed list; empty/null stays on the catch-all
final class PacketSendIndex<T> {
    private final Map<PacketTypeCommon, T[]> byType;
    private final T[] always;

    PacketSendIndex(List<T> listeners, Function<T, PacketTypeCommon[]> types, IntFunction<T[]> array) {
        Map<PacketTypeCommon, List<T>> tmp = new IdentityHashMap<>();
        List<T> catchAll = new ArrayList<>();

        for (T listener : listeners) {
            PacketTypeCommon[] declared = types.apply(listener);
            if (declared == null || declared.length == 0) {
                catchAll.add(listener);
                continue;
            }
            for (PacketTypeCommon type : declared) {
                if (type == null) continue;
                tmp.computeIfAbsent(type, ignored -> new ArrayList<>()).add(listener);
            }
        }

        Map<PacketTypeCommon, T[]> built = new IdentityHashMap<>(tmp.size());
        tmp.forEach((type, list) -> built.put(type, list.toArray(array)));
        this.byType = built;
        this.always = catchAll.toArray(array);
    }

    void dispatch(PacketSendEvent event, Consumer<T> call) {
        T[] typed = byType.get(event.getPacketType());
        if (typed != null) {
            for (T listener : typed) {
                call.accept(listener);
            }
        }
        for (T listener : always) {
            call.accept(listener);
        }
    }

    int typedTypeCount() {
        return byType.size();
    }

    int catchAllCount() {
        return always.length;
    }
}
