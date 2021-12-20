package ac.grim.grimac.utils.blockplace;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.collisions.AxisUtil;
import ac.grim.grimac.utils.nmsutil.XMaterial;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.*;

// Holy shit mojang stop reusing packets like this
// for fucks sake there are several desyncs AGAIN???
// HOW DIFFICULT CAN IT BE TO TELL THE SERVER THAT YOU RANG A BELL, AND NOT CREATE A GHOST BLOCK???
public class ConsumesBlockPlace {
    public static boolean consumesPlace(GrimPlayer player, BaseBlockState state, BlockPlace place) {
        if (state instanceof FlatBlockState) {
            return consumesPlaceFlat(player, (FlatBlockState) state, place);
        }
        return consumesPlaceMagic(player, (MagicBlockState) state, place);
    }

    private static boolean consumesPlaceFlat(GrimPlayer player, FlatBlockState state, BlockPlace place) {
        BlockData data = state.getBlockData();
        // Hey look, it's another DESYNC MOJANG
        if (data instanceof Bell) {
            Bell bell = (Bell) data;
            return goodBellHit(player, bell, place);
        }
        if (data.getMaterial() == XMaterial.CANDLE_CAKE.parseMaterial()) {
            Cake cake = (Cake) Material.CAKE.createBlockData();
            cake.setBites(1);
            place.set(cake);
        }
        if (data instanceof Cake) {
            Cake cake = (Cake) data;
            if (cake.getBites() == 0 && place.getMaterial() != null && place.getMaterial().name().endsWith("CANDLE")) {
                place.set(XMaterial.CANDLE_CAKE.parseMaterial());
                return true;
            }

            if (player.gamemode == GameMode.CREATIVE || player.bukkitPlayer.getFoodLevel() < 20) {
                if (cake.getBites() + 1 != 7) {
                    Cake clone = (Cake) cake.clone();
                    clone.setBites(cake.getBites() + 1);
                    place.set(clone);
                } else {
                    place.set(Material.AIR);
                }
                return true;
            }

            return false;
        }
        if (data instanceof CaveVinesPlant) {
            CaveVinesPlant vines = (CaveVinesPlant) data;
            if (vines.isBerries()) {
                CaveVinesPlant clone = ((CaveVinesPlant) vines.clone());
                clone.setBerries(false);
                place.set(clone);
                return true;
            }
            return false;
        }
        if (data instanceof Ageable && data.getMaterial() == XMaterial.SWEET_BERRY_BUSH.parseMaterial()) {
            Ageable ageable = (Ageable) data;
            if (ageable.getAge() != 3 && place.getMaterial() == Material.BONE_MEAL) {
                return false;
            } else if (ageable.getAge() > 1) {
                Ageable clone = (Ageable) data.clone();
                clone.setAge(1);
                place.set(clone);
                return true;
            } else {
                return false;
            }
        }
        if (data.getMaterial() == Material.TNT) {
            return place.getMaterial() == Material.FIRE_CHARGE || place.getMaterial() == Material.FLINT_AND_STEEL;
        }
        if (data instanceof RespawnAnchor) {
            if (place.getMaterial() == Material.GLOWSTONE) {
                return true;
            }
            return player.bukkitPlayer.getInventory().getItemInOffHand().getType() != Material.GLOWSTONE;
        }
        if (data instanceof CommandBlock || data instanceof Jigsaw || data instanceof StructureBlock) {
            // Where is the permission level???? Check for >= 2 level eventually... no API for this.
            // Only affects OP players, will fix eventually (also few desyncs from no minecraft lag compensation)
            return player.bukkitPlayer.isOp() && player.gamemode == GameMode.CREATIVE;
        }
        if (data.getMaterial() == XMaterial.COMPOSTER.parseMaterial() && data instanceof Levelled) {
            Levelled levelled = (Levelled) data;
            if (XMaterial.isCompostable(XMaterial.fromMaterial(place.getMaterial())) && levelled.getLevel() < 8) {
                return true;
            }
            return levelled.getLevel() == 8;
        }
        if (data instanceof Jukebox) {
            Jukebox jukebox = (Jukebox) data;
            return jukebox.hasRecord();
        }
        if (data instanceof Lectern) {
            Lectern lectern = (Lectern) data;
            if (lectern.hasBook()) return true;
            return Tag.ITEMS_LECTERN_BOOKS.isTagged(place.getMaterial());
        }

        return false;
    }

    private static boolean goodBellHit(GrimPlayer player, Bell bell, BlockPlace place) {
        BlockFace direction = place.getDirection();
        return isProperHit(bell, direction, place.getHitData().getRelativeBlockHitLocation().getY());
    }

    private static boolean isProperHit(Bell bell, BlockFace direction, double p_49742_) {
        if (direction != BlockFace.UP && direction != BlockFace.DOWN && !(p_49742_ > (double) 0.8124F)) {
            org.bukkit.block.BlockFace dir = bell.getFacing();
            Bell.Attachment attachment = bell.getAttachment();
            org.bukkit.block.BlockFace dir2 = org.bukkit.block.BlockFace.valueOf(direction.name());

            switch (attachment) {
                case FLOOR:
                    return AxisUtil.isSameAxis(dir, dir2);
                case SINGLE_WALL:
                case DOUBLE_WALL:
                    return !AxisUtil.isSameAxis(dir, dir2);
                case CEILING:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    private static boolean consumesPlaceMagic(GrimPlayer player, MagicBlockState state, BlockPlace place) {
        // Hey look, it's another DESYNC MOJANG
        if (state.getMaterial() == Material.CAKE) {
            if (state.getBlockData() == 0 && place.getMaterial() != null && place.getMaterial().name().endsWith("CANDLE")) {
                place.set(XMaterial.CANDLE_CAKE.parseMaterial());
                return true;
            }

            if (player.gamemode == GameMode.CREATIVE || player.bukkitPlayer.getFoodLevel() < 20) {
                if (state.getBlockData() + 1 != 8) {
                    place.set(new MagicBlockState(Material.CAKE.getId(), state.getBlockData() + 1));
                } else {
                    place.set(Material.AIR);
                }
                return true;
            }

            return false;
        }
        if (state.getMaterial() == Material.TNT) {
            return place.getMaterial() == Material.FIRE_CHARGE || place.getMaterial() == Material.FLINT_AND_STEEL;
        }
        if (state.getMaterial() == Material.COMMAND_BLOCK || state.getMaterial() == Material.STRUCTURE_BLOCK) {
            // Where is the permission level???? Check for >= 2 level eventually... no API for this.
            // Only affects OP players, will fix eventually (also few desyncs from no minecraft lag compensation)
            return player.bukkitPlayer.isOp() && player.gamemode == GameMode.CREATIVE;
        }
        if (state.getMaterial() == Material.JUKEBOX) { // Has disc
            return (state.getBlockData() & 0x1) == 0x1;
        }

        return false;
    }
}
