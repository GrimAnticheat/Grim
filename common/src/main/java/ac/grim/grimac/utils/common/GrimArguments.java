package ac.grim.grimac.utils.common;

public class GrimArguments {

    public static final boolean TRANSACTION_KICKS = !Boolean.getBoolean("grim.disable-transaction-kick");
    public static final String API_URL = System.getProperty("grim.api-url", "https://api.grim.ac/v1/server/");
    public static final String PASTE_URL = System.getProperty("grim.paste-url", "https://paste.grim.ac/");

}
