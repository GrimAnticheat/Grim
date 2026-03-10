package ac.grim.grimac.utils.common;

import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.experimental.UtilityClass;

import java.io.InputStream;
import java.util.Properties;

@UtilityClass
public class PropertiesUtil {

    public static Properties readProperties(Class<?> clazz, String path) {
        final Properties properties = new Properties();
        try (InputStream inputStream = clazz.getClassLoader().getResourceAsStream(path)) {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                throw new RuntimeException("Cannot find properties file: " + path);
            }
        } catch (Exception e) {
            LogUtil.error(e);
        }
        return properties;
    }

    public static String getPropertyOrElse(Properties properties, String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

}
