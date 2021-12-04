package ac.grim.grimac.utils.chunkdata.eighteen;

import ac.grim.grimac.utils.chunkdata.sixteen.BitStorage;

public class SingletonStorage extends BitStorage {
    public SingletonStorage() {
        super();
    }

    @Override
    public int get(int index) {
        return 0;
    }

    @Override
    public void set(int index, int value) {
    }
}
