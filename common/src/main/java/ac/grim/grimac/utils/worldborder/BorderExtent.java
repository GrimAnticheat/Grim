package ac.grim.grimac.utils.worldborder;

public interface BorderExtent {
    double size();

    double getMinX(double centerX, double absoluteMaxSize);

    double getMaxX(double centerX, double absoluteMaxSize);

    double getMinZ(double centerZ, double absoluteMaxSize);

    double getMaxZ(double centerZ, double absoluteMaxSize);

    BorderExtent tick();

    BorderExtent update();
}
