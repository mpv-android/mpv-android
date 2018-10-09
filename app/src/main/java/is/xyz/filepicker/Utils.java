package is.xyz.filepicker;

import androidx.annotation.NonNull;

/**
 * Some utility methods
 */
public class Utils {

    private static final String SEP = "/";

    /**
     * Append the second pathString to the first. The result will not end with a /.
     * In case two absolute paths are given, e.g. /A/B/, and /C/D/, then the result
     * will be /A/B/C/D
     *
     * Multiple slashes will be shortened to a single slash, so /A///B is equivalent to /A/B
     */
    @NonNull
    public static String appendPath(@NonNull String first,
                                    @NonNull String second) {
        String result = first + SEP + second;

        while (result.contains("//")) {
            result = result.replaceAll("//", "/");
        }

        if (result.length() > 1 && result.endsWith(SEP)) {
            return result.substring(0, result.length() - 1);
        } else {
            return result;
        }
    }
}
