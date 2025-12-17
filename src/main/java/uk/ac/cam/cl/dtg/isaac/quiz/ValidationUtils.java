package uk.ac.cam.cl.dtg.isaac.quiz;

import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static java.lang.Math.max;
import static java.lang.Math.min;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES;

/**
 * Utility class for validation of various types of input.
 */
public final class ValidationUtils {

    /**
     * A class to represent the significant figures a number has, noting if it is ambiguous and the range if so.
     */
    public static class SigFigResult {
        boolean isAmbiguous;
        int sigFigsMin;
        int sigFigsMax;

        /**
         * Default constructor.
         *
         * @param isAmbiguous - whether the significant figures are ambiguous or not.
         * @param sigFigsMin  - the minimum number of sig figs the number could have
         * @param sigFigsMax  - the maximum number of sig fig the number could have, equal to min if not ambiguous.
         */
        SigFigResult(final boolean isAmbiguous, final int sigFigsMin, final int sigFigsMax) {
            this.isAmbiguous = isAmbiguous;
            this.sigFigsMin = sigFigsMin;
            this.sigFigsMax = sigFigsMax;
        }
    }

    /* Many users are getting answers wrong solely because we don't allow their (unambiguous) syntax for 10^x. Be nicer!
       Allow spaces either side of the times and allow * x X × and \times as multiplication!
       Also allow ^ or ** for powers. Allow e or E. Allow optional brackets around the powers of 10.
       Extract exponent as either group <exp1> or <exp2> (the other will become '').

       Inputs of style "1x10^3" and of style "10^3" must be dealt with separately, since for the latter we need
       to add a "1" to the start so both can become "1e3" when replacing the 10 part.
     */
    private static final String PREFIXED_POWER_OF_TEN_REGEX = "[ ]?((\\*|x|X|×|\\\\times)[ ]?10(\\^|\\*\\*)|e|E)([({](?<exp1>-?[0-9]+)[)}]|(?<exp2>-?[0-9]+))";
    private static final String BARE_POWER_OF_TEN_REGEX = "^(10(\\^|\\*\\*))([({](?<exp1>-?[0-9]+)[)}]|(?<exp2>-?[0-9]+))$";

    private ValidationUtils() {
        // Utility class
    }

    public enum ComparisonType {
        LESS_THAN,
        GREATER_THAN,
        EQUAL_TO
    }

    static boolean compareNumericValues(final String trustedValue, final String untrustedValue,
                                        final Integer significantFiguresRequired,
                                        final ComparisonType comparisonType, final Logger log
    ) throws NumberFormatException {
        log.debug("\t[numericValuesMatch]");
        double trustedDouble, untrustedDouble;

        String untrustedParsedValue = reformatNumberForParsing(untrustedValue);
        String trustedParsedValue = reformatNumberForParsing(trustedValue);

        if (null == significantFiguresRequired) {
            trustedDouble = stringValueToDouble(trustedParsedValue, log);
            untrustedDouble = stringValueToDouble(untrustedParsedValue, log);
        } else {
            // Round to N s.f.
            trustedDouble = roundStringValueToSigFigs(trustedParsedValue, significantFiguresRequired, log);
            untrustedDouble = roundStringValueToSigFigs(untrustedParsedValue, significantFiguresRequired, log);
        }

        final double epsilon = 1e-50;

        final double threshold = max(epsilon * max(trustedDouble, untrustedDouble), epsilon);

        switch (comparisonType) {
            case EQUAL_TO:
            default:
                return Math.abs(trustedDouble - untrustedDouble) < threshold;
            case LESS_THAN:
                return trustedDouble < untrustedDouble && Math.abs(trustedDouble - untrustedDouble) > threshold;
            case GREATER_THAN:
                return trustedDouble > untrustedDouble && Math.abs(trustedDouble - untrustedDouble) > threshold;
        }
    }

    /**
     * Test whether two quantity values match. Parse the strings as doubles, supporting notation of 3x10^12 to mean
     * 3e12, then test that they match to given number of s.f.
     *
     * @param trustedValue               - first number
     * @param untrustedValue             - second number
     * @param significantFiguresRequired - the number of significant figures to perform comparisons to (can be null, in
     *                                   which case exact comparison is performed)
     * @param log                        - logger
     * @return true when the numbers match
     * @throws NumberFormatException - when one of the values cannot be parsed
     */
    static boolean numericValuesMatch(final String trustedValue, final String untrustedValue,
                                      final Integer significantFiguresRequired, final Logger log
    ) throws NumberFormatException {
        return compareNumericValues(trustedValue, untrustedValue, significantFiguresRequired, ComparisonType.EQUAL_TO, log);
    }

    /**
     * Return a double equivalent to value.
     *
     * @param value - number, as String, to convert to double
     * @param log - logger for debug tracing
     * @return the converted number.
     */
    private static double stringValueToDouble(final String value, final Logger log) {
        log.debug("\t[stringValueToDouble]");
        return stringValueToDouble(value);
    }

    /**
     * Return a double equivalent to value.
     *
     * @param value - number, as String, to convert to double
     * @return the converted number.
     */
    public static double stringValueToDouble(final String value) {
        return new BigDecimal(value).doubleValue();
    }

    /**
     * Round a double to a given number of significant figures.
     *
     * @param value   - number to round
     * @param sigFigs - number of significant figures required
     * @param log     - logger
     * @return the rounded number.
     */
    static double roundStringValueToSigFigs(final String value, final int sigFigs, final Logger log) {
        log.debug("\t[roundStringValueToSigFigs]");

        // To prevent floating point arithmetic errors when rounding the value, use a BigDecimal and pass the string
        // value of the number:
        BigDecimal bigDecimalValue = new BigDecimal(value);
        BigDecimal rounded = bigDecimalValue.round(new MathContext(sigFigs, RoundingMode.HALF_UP));

        return rounded.doubleValue();
    }

    /**
     * Format a number in string form such that Java BigDecimal can parse it. Trims leading and trailing spaces.
     *
     * Replace "x10^(...)" with "e(...)", allowing many common unambiguous cases, and fix uses of Unicode minus signs,
     * and allow bare powers of ten.
     *
     * @param numberToFormat - number in some unambiguous standard form.
     * @return - number in engineering standard form e.g. "3.4e3"
     */
    public static String reformatNumberForParsing(final String numberToFormat) {
        String reformattedNumber = numberToFormat.trim().replace("−", "-");
        reformattedNumber = reformattedNumber.replaceFirst(PREFIXED_POWER_OF_TEN_REGEX, "e${exp1}${exp2}");
        reformattedNumber = reformattedNumber.replaceFirst(BARE_POWER_OF_TEN_REGEX, "1e${exp1}${exp2}");
        return reformattedNumber;
    }

    /**
     * Deduce from the user answer and question data how many sig figs we should use when checking a question. We must
     * pick a value in the allowed range, but it may be informed by the user's answer.
     *
     * @param valueToCheck      - the user provided value in string form
     * @param minAllowedSigFigs - the minimum number of significant figures the question allows
     * @param maxAllowedSigFigs - the maximum number of significant figures the question allows
     * @param log               - logger
     * @return the number of significant figures that should be used when checking the question
     */
    static int numberOfSignificantFiguresToValidateWith(final String valueToCheck, final Integer minAllowedSigFigs,
                                                         final Integer maxAllowedSigFigs, final Logger log) {
        log.debug("\t[numberOfSignificantFiguresToValidateWith]");
        int untrustedValueSigFigs;
        SigFigResult sigFigsFromUser = extractSignificantFigures(valueToCheck, log);

        if (!sigFigsFromUser.isAmbiguous) {
            untrustedValueSigFigs = sigFigsFromUser.sigFigsMin;
        } else {
            // Since choosing the least possible number of sig figs gives the loosest comparison, use that.
            // This is kindest to the user, but may need to be revised.
            untrustedValueSigFigs = sigFigsFromUser.sigFigsMin;
        }

        /* The number of significant figures to validate to must be less than or equal to the max allowed, and greater
           than or equal to the minimum allowed. If the ranges intersect, or the untrusted value is unambiguous in the
           acceptable range, choose the least number of sig figs the user answer allows; this is kindest to the user
           in terms of matching known wrong answers.
         */
        return max(
                min(untrustedValueSigFigs, Objects.requireNonNullElse(maxAllowedSigFigs, NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES)),
                Objects.requireNonNullElse(minAllowedSigFigs, NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES)
        );
    }


    /**
     * Extract from a number in string form how many significant figures it is given to, noting the range if it is
     * ambiguous (as in the case of 1000, for example).
     *
     * @param valueToCheck - the user provided value in string form
     * @param log          - logger
     * @return a SigFigResult containing info on the sig figs of the number
     */
    static SigFigResult extractSignificantFigures(final String valueToCheck, final Logger log) {
        log.debug("\t[extractSignificantFigures]");
        String untrustedParsedValue = ValidationUtils.reformatNumberForParsing(valueToCheck);

        // Parse exactly into a BigDecimal:
        BigDecimal bd = new BigDecimal(untrustedParsedValue);

        if (untrustedParsedValue.contains(".")) {
            // If it contains a decimal point then there is no ambiguity in how many sig figs it has.
            return new ValidationUtils.SigFigResult(false, bd.precision(), bd.precision());
        } else {
            // If not, we have to be flexible because integer values have undefined significant figure rules.
            char[] unscaledValueToCheck = bd.unscaledValue().toString().toCharArray();

            // Counting trailing zeroes is useful to give bounds on the number of sig figs it could be to:
            int trailingZeroes = 0;
            for (int i = unscaledValueToCheck.length - 1; i >= 0; i--) {
                if (unscaledValueToCheck[i] == '0') {
                    trailingZeroes++;
                } else {
                    break;
                }
            }

            if (trailingZeroes == 0) {
                // This is an integer with no trailing zeroes; there is no ambiguity in how many sig figs it has.
                return new ValidationUtils.SigFigResult(false, bd.precision(), bd.precision());
            } else {
                // This is an integer with one or more trailing zeroes; it is unclear how many sig figs it may be to.
                int untrustedValueMinSigFigs = bd.precision() - trailingZeroes;
                int untrustedValueMaxSigFigs = bd.precision();
                return new ValidationUtils.SigFigResult(true, untrustedValueMinSigFigs, untrustedValueMaxSigFigs);
            }
        }
    }

    /**
     * Verify if an answer given is to too few significant figures.
     *
     * @param valueToCheck      - the value as a string from the user to check.
     * @param minAllowedSigFigs - the minimum number of significant figures that is expected for the answer to be correct.
     * @return true if too few, false if not.
     */
    public static boolean tooFewSignificantFigures(final String valueToCheck, final int minAllowedSigFigs, final Logger log) {
        log.debug("\t[tooFewSignificantFigures]");

        ValidationUtils.SigFigResult sigFigsFromUser = ValidationUtils.extractSignificantFigures(valueToCheck, log);

        return sigFigsFromUser.sigFigsMax < minAllowedSigFigs;
    }

    /**
     * Verify if an answer given is to too many significant figures.
     *
     * @param valueToCheck      - the value as a string from the user to check.
     * @param maxAllowedSigFigs - the maximum number of significant figures that is expected for the answer to be correct.
     * @return true if too many, false if not.
     */
    public static boolean tooManySignificantFigures(final String valueToCheck, final int maxAllowedSigFigs, final Logger log) {
        log.debug("\t[tooManySignificantFigures]");

        ValidationUtils.SigFigResult sigFigsFromUser = ValidationUtils.extractSignificantFigures(valueToCheck, log);

        return sigFigsFromUser.sigFigsMin > maxAllowedSigFigs;
    }

    /** Validate a set of rules. Each rule takes two arguments. `.add` some rules, then `.check` whether they held.*/
    public static class BiRuleValidator<T, U> {
        protected final List<Rule<T, U>> rules = new ArrayList<>();

        public BiRuleValidator<T, U> add(final String message, final BiPredicate<T, U> rule) {
            rules.add(new Rule<>(message, rule));
            return this;
        }

        /** Add rules from another RuleValidator. */
        public BiRuleValidator<T, U> addRulesFrom(final RuleValidator<T> validator) {
            validator.rules.forEach(r -> add(r.message, (t, u) -> r.predicate.test(t, null)));
            return this;
        }

        /** Add rules from another BiRuleValidator. */
        public BiRuleValidator<T, U> addRulesFrom(final BiRuleValidator<T, U> validator) {
            validator.rules.forEach(r -> add(r.message, r.predicate));
            return this;
        }

        /** Apply the validation rules on a set of objects. */
        public Optional<String> check(final T t, final U u) {
            return rules.stream()
                .filter(r -> r.predicate.test(t, u))
                .map(r -> r.message)
                .findFirst();
        }

        /** A rule used by either a BiRuleValidator or a RuleValidator. */
        protected static class Rule<T, U> {
            public final String message;
            public final BiPredicate<T, U> predicate;

            public Rule(final String message, final BiPredicate<T, U> predicate) {
                this.message = message;
                this.predicate = predicate;
            }
        }
    }

    /** A specialized BiRuleValidator whose rules take a single argument. */
    public static class RuleValidator<T> extends BiRuleValidator<T, Void> {
        public RuleValidator<T> add(final String message, final Predicate<T> rule) {
            super.add(message, (t, ignored) -> rule.test(t));
            return this;
        }

        public Optional<String> check(final T t) {
            return super.check(t, null);
        }
    }
}
