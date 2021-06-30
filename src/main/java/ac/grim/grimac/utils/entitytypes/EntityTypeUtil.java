/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ac.grim.grimac.utils.entitytypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EntityTypeUtil {

    /**
     * Returns an ordered array with each index representing the actual entity id.
     *
     * @param values entity types
     * @return ordered array with each index representing the actual entity id
     */
    public static EntityType[] toOrderedArray(EntityType[] values) {
        List<EntityType> types = new ArrayList<>();
        for (EntityType type : values) {
            if (type.getId() != -1) {
                types.add(type);
            }
        }

        types.sort(Comparator.comparingInt(EntityType::getId));
        return types.toArray(new EntityType[0]);
    }

    /**
     * Returns the entity type from id, or the given fallback if out of bounds.
     *
     * @param values   sorted entity type array
     * @param typeId   entity type id
     * @param fallback fallback/base entity type
     * @return entity type from id
     */
    public static EntityType getTypeFromId(EntityType[] values, int typeId, EntityType fallback) {
        EntityType type;
        if (typeId < 0 || typeId >= values.length || (type = values[typeId]) == null) {
            return fallback;
        }
        return type;
    }
}