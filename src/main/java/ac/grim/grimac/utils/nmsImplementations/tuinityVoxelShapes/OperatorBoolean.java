package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

public interface OperatorBoolean {
    OperatorBoolean FALSE = (var0, var1) -> {
        return false;
    };
    OperatorBoolean NOT_OR = (var0, var1) -> {
        return !var0 && !var1;
    };
    OperatorBoolean ONLY_SECOND = (var0, var1) -> {
        return var1 && !var0;
    };
    OperatorBoolean NOT_FIRST = (var0, var1) -> {
        return !var0;
    };
    OperatorBoolean ONLY_FIRST = (var0, var1) -> {
        return var0 && !var1;
    };
    OperatorBoolean NOT_SECOND = (var0, var1) -> {
        return !var1;
    };
    OperatorBoolean NOT_SAME = (var0, var1) -> {
        return var0 != var1;
    };
    OperatorBoolean NOT_AND = (var0, var1) -> {
        return !var0 || !var1;
    };
    OperatorBoolean AND = (var0, var1) -> {
        return var0 && var1;
    };
    OperatorBoolean SAME = (var0, var1) -> {
        return var0 == var1;
    };
    OperatorBoolean SECOND = (var0, var1) -> {
        return var1;
    };
    OperatorBoolean CAUSES = (var0, var1) -> {
        return !var0 || var1;
    };
    OperatorBoolean FIRST = (var0, var1) -> {
        return var0;
    };
    OperatorBoolean CAUSED_BY = (var0, var1) -> {
        return var0 || !var1;
    };
    OperatorBoolean OR = (var0, var1) -> {
        return var0 || var1;
    };
    OperatorBoolean TRUE = (var0, var1) -> {
        return true;
    };

    boolean apply(boolean var1, boolean var2);
}

