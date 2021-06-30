package ac.grim.grimac.utils.entitytypes;

import javax.annotation.Nullable;

public interface EntityType {

    /**
     * Returns the entity id.
     *
     * @return entity id
     */
    int getId();

    /**
     * Returns the parent entity type if present.
     *
     * @return parent entity type if present
     */
    @Nullable
    EntityType getParent();

    /**
     * Returns the entity type name, not necessarily matching the Vanilla type name.
     *
     * @return entity type name
     */
    String name();

    default boolean is(EntityType... types) {
        for (EntityType type : types) {
            if (this == type) {
                return true;
            }
        }
        return false;
    }

    default boolean is(EntityType type) {
        return this == type;
    }

    /**
     * Returns whether the current type is equal to the given type, or has it as a parent type.
     *
     * @param type entity type to check against
     * @return true if the current type is equal to the given type, or has it as a parent type
     */
    default boolean isOrHasParent(EntityType type) {
        EntityType parent = this;

        do {
            if (parent == type) {
                return true;
            }

            parent = parent.getParent();
        } while (parent != null);

        return false;
    }
}