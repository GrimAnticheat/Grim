package ac.grim.grimac.utils.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GrimArguments {
    public static final boolean TRANSACTION_KICKS = !Boolean.getBoolean("grim.disable-transaction-kick");
    public static final String API_URL = System.getProperty("grim.api-url", "https://api.grim.ac/v1/server/");
    public static final String PASTE_URL = System.getProperty("grim.paste-url", "https://paste.grim.ac/");
    public static final String PLATFORM_OVERRIDE = System.getProperty("grim.platform-override", "");
}
