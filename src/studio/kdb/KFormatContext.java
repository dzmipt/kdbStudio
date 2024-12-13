package studio.kdb;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class KFormatContext {

    private final static NumberFormat RAW_FORMAT = new DecimalFormat("#.#######");
    private final static NumberFormat COMMA_FORMAT = new DecimalFormat("#,###.#######");

    static {
        setMaxFractionDigits(Config.getInstance().getInt(Config.MAX_FRACTION_DIGITS));
    }

    private boolean showType;
    private boolean showThousandsComma;
    private final Rounding rounding;

    public enum Rounding {
        NO (false, false, false, false),
        DAY (true, true, true, true),
        MINUTE (false, true, true, true),
        SECOND (false, false, true, true),
        MILLIS (false, false, false, true);

        private final boolean dd, mm, ss, ms;

        Rounding(boolean dd, boolean mm, boolean ss, boolean ms) {
            this.dd = dd;
            this.mm = mm;
            this.ss = ss;
            this.ms = ms;
        }
        public boolean days() {
            return dd;
        }

        public boolean minutes() {
            return mm;
        }

        public boolean seconds() {
            return ss;
        }

        public boolean millis() {
            return ms;
        }
    };


    public final static KFormatContext DEFAULT = new KFormatContext();
    public final static KFormatContext NO_TYPE = new KFormatContext(false, false);

    public final static KFormatContext DAY = new KFormatContext(Rounding.DAY);
    public final static KFormatContext MINUTE = new KFormatContext(Rounding.MINUTE);
    public final static KFormatContext SECOND = new KFormatContext(Rounding.SECOND);
    public final static KFormatContext MILLIS = new KFormatContext(Rounding.MILLIS);

    public static void setMaxFractionDigits(int maxFractionDigits) {
        RAW_FORMAT.setMaximumFractionDigits(maxFractionDigits);
        COMMA_FORMAT.setMaximumFractionDigits(maxFractionDigits);
    }

    public KFormatContext(boolean showType, boolean showThousandsComma, Rounding rounding) {
        this.showType = showType;
        this.showThousandsComma = showThousandsComma;
        this.rounding = rounding;
    }

    public KFormatContext(Rounding rounding) {
        this(false, false, rounding);
    }

    public KFormatContext(boolean showType, boolean showThousandsComma) {
        this(showType, showThousandsComma, Rounding.NO);
    }

    public KFormatContext(KFormatContext formatContext) {
        this(formatContext.showType, formatContext.showThousandsComma);
    }

    public KFormatContext() {
        this(true, false);
    }

    public Rounding getRounding() {
        return rounding;
    }

    public NumberFormat getNumberFormat() {
        return showThousandsComma ? COMMA_FORMAT : RAW_FORMAT;
    }

    public boolean showType() {
        return showType;
    }

    public KFormatContext setShowType(boolean showType) {
        this.showType = showType;
        return this;
    }

    public boolean showThousandsComma() {
        return showThousandsComma;
    }

    public KFormatContext setShowThousandsComma(boolean showThousandsComma) {
        this.showThousandsComma = showThousandsComma;
        return this;
    }
}
