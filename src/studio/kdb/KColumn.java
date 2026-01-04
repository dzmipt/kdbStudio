package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KColumn {

    private final String name;
    private final K.Indexable data;
    private final K.KBase atom;
    private final KType type;
    private final int len;


    private final static Logger log = LogManager.getLogger();

    public KColumn(String name, K.KBase k, int len) {
        this.name = name;

        if (k instanceof K.Indexable) {
            data = (K.Indexable) k;
            atom = null;
            if (k.count() != len) {
                log.error("KColumn: different length. Passed {}; data length is {}", len, k.count());
            }
            this.len = k.count();
            if (k instanceof K.Flip) {
                type = KType.Dict;
                log.warn("Unusual column with type dictionary");
            } else if (k instanceof K.KBaseVector) {
                type = k.getType().getElementType();
            } else {
                throw new IllegalArgumentException("Internal misconfiguration for type: " + k.getType());
            }
        } else {
            log.warn("Unusual column as atom");
            data = null;
            atom = k;
            this.len = len;
            this.type = k.getType();
        }
    }

    public String getName() {
        return name;
    }

    public KType getElementType() {
        return type;
    }

    public K.KBase get(int index) {
        if (data != null) return data.at(index);
        else return atom;
    }

    public int size() {
        return len;
    }

}
