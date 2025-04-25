package ac.grim.grimac.utils.lists;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

// https://github.com/ThomasOM/Pledge/blob/master/src/main/java/dev/thomazz/pledge/util/collection/ListWrapper.java
@Getter
@RequiredArgsConstructor
public abstract class ListWrapper<T> implements List<T> {
    protected final List<T> base;
}
