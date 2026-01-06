package ac.grim.grimac.utils.lists;

import com.github.retrooper.packetevents.protocol.world.states.type.StateType;

import java.util.Collection;

public class ArrayUtils {

    public static StateType[] combine(Collection<StateType> tagStates, StateType... manualStates) {
        StateType[] result = new StateType[tagStates.size() + manualStates.length];
        int i = 0;
        for (StateType state : tagStates) {
            result[i++] = state;
        }
        System.arraycopy(manualStates, 0, result, tagStates.size(), manualStates.length);
        return result;
    }
}
