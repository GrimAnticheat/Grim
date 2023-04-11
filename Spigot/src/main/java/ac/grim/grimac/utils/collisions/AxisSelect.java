package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;

public interface AxisSelect {
    SimpleCollisionBox modify(SimpleCollisionBox box);
}