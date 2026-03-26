package uk.ac.cam.cl.dtg.util;

import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.util.Comparator;
import java.util.List;

/**
 *  A utility class to order names lexicographically.
 */
public class NameOrderer {

    /**
     *  It does not make sense to create one of these!
     */
    private NameOrderer() {
    }

    /**
     * Helper method to consistently sort users by given name then family name in a case-insensitive order.
     * @param users
     *            - list of users.
     */
    public static void orderUsersByName(final List<RegisteredUserDTO> users) {
        // Remove apostrophes so that string containing them are ordered in the same way as in Excel.
        // I.e. we want that "O'Aaa" < "Obbb" < "O'Ccc"
        Comparator<String> excelStringOrder = Comparator.nullsLast((String a, String b) ->
                String.CASE_INSENSITIVE_ORDER.compare(a.replaceAll("'", ""), b.replaceAll("'", "")));

        // If names differ only by an apostrophe (i.e. "O'A" and "Oa"), break ties using name including any apostrophes:
        users.sort(Comparator
                .comparing(RegisteredUserDTO::getFamilyName, excelStringOrder)
                .thenComparing(RegisteredUserDTO::getGivenName, excelStringOrder)
                .thenComparing(RegisteredUserDTO::getFamilyName));
    }
}
