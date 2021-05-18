package ac.grim.grimac.utils.blockdata;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;

public class WrappedBlockDataValue {
    public void getWrappedData(FlatBlockState data) {

    }

    public void getWrappedData(MagicBlockState data) {

    }

    public void getData(BaseBlockState data) {
        if (data instanceof FlatBlockState) {
            getWrappedData((FlatBlockState) data);
        } else {
            getWrappedData((MagicBlockState) data);
        }
    }
}
