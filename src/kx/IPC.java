package kx;

import studio.kdb.K;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/*
types
20+ userenums
98 table
99 dict
100 lambda
101 unary prim
102 binary prim
103 ternary(operator)
104 projection
105 composition
106 f'
107 f/
108 f\
109 f':
110 f/:
111 f\:
112 dynamic load
 */

public class IPC {

    public final static Charset ENCODING = StandardCharsets.UTF_8;

    public static KMessage deserialise(byte[] data, boolean compressed, boolean isLittleEndian) {
        return new IPC(data, 0, compressed, isLittleEndian).deserialise();
    }

    private byte[] b;
    private int j;
    private boolean a;

    IPC(byte[] data, int offset, boolean compressed, boolean isLittleEndian) {
        b = data;
        a = isLittleEndian;
        j = offset;
        if (compressed) {
            b = uncompress();
            j = 8;
        }
    }

    public KMessage deserialise() {
        try {
            if (b[0] == -128) {
                j = 1;
                return new KMessage(new K4Exception(rs().getString()));
            }
            return new KMessage(r());
        } catch (UnsupportedEncodingException e) {
            return new KMessage(e);
        }
    }

    private byte[] uncompress() {
        int n = 0, r = 0, f = 0, s = 8, p = s;
        short i = 0;
        byte[] dst = new byte[ri()];
        int d = j;
        int[] aa = new int[256];
        while (s < dst.length) {
            if (i == 0) {
                f = 0xff & (int) b[d++];
                i = 1;
            }
            if ((f & i) != 0) {
                r = aa[0xff & (int) b[d++]];
                dst[s++] = dst[r++];
                dst[s++] = dst[r++];
                n = 0xff & (int) b[d++];
                for (int m = 0; m < n; m++) {
                    dst[s + m] = dst[r + m];
                }
            } else {
                dst[s++] = b[d++];
            }
            while (p < s - 1) {
                aa[(0xff & (int) dst[p]) ^ (0xff & (int) dst[p + 1])] = p++;
            }
            if ((f & i) != 0) {
                p = s += n;
            }
            i *= 2;
            if (i == 256) {
                i = 0;
            }
        }
        return dst;
    }

    K.KBase r() throws UnsupportedEncodingException {
        int i = 0, n, t = b[j++];
        if (t < 0)
            switch (t) {
                case -1:
                    return new K.KBoolean(rb());
                case -2:
                    return new K.KGuid(rg());
                case -4:
                    return new K.KByte(b[j++]);
                case -5:
                    return new K.KShort(rh());
                case -6:
                    return new K.KInteger(ri());
                case -7:
                    return new K.KLong(rj());
                case -8:
                    return new K.KFloat(re());
                case -9:
                    return new K.KDouble(rf());
                case -10:
                    return new K.KCharacter(rc());
                case -11:
                    return rs();
                case -12:
                    return rp();
                case -13:
                    return rm();
                case -14:
                    return rd();
                case -15:
                    return rz();
                case -16:
                    return rn();
                case -17:
                    return ru();
                case -18:
                    return rv();
                case -19:
                    return rt();
            }

        if (t == 100)
            return rfn(); // fn - lambda
        if (t == 101)
            return rup();  // unary primitive
        if (t == 102)
            return rbp();  // binary primitive
        if (t == 103)
            return rternary();
        if (t == 104)
            return rproj(); // fn projection
        if (t == 105)
            return rcomposition();

        if (t == 106)
            return rfeach(); // f'
        if (t == 107)
            return rfover(); // f/
        if (t == 108)
            return rfscan(); //f\
        if (t == 109)
            return rfPrior(); // f':
        if (t == 110)
            return rfEachRight(); // f/:
        if (t == 111)
            return rfEachLeft(); // f\:
        if (t == 112) {
            // dynamic load
            j++;
            return null;
        }
        if (t == 127) {
            K.Dict d = new K.Dict(r(), r());
            d.setAttr((byte) 1);
            return d;
        }
        if (t > 99) {
            j++;
            return null;
        }
        if (t == 99)
            return new K.Dict(r(), r());
        byte attr = b[j++];
        if (t == 98) {
            K.Dict d = (K.Dict)r();
            if (d.x instanceof K.KSymbolVector && d.y instanceof K.KBaseVector) {
                return new K.Flip((K.KSymbolVector)d.x, (K.KBaseVector<? extends K.KBase>)d.y);
            } else {
                return new K.MappedTable(d);
            }
        }        n = ri();
        switch (t) {
            case 0: {
                K.KBase[] array = new K.KBase[n];
                for (; i < n; i++)
                    array[i] = r();
                K.KList L = new K.KList(array);
                L.setAttr(attr);
                return L;
            }
            case 1: {
                boolean[] array = new boolean[n];
                for (; i < n; i++)
                    array[i] = rb();
                K.KBooleanVector B = new K.KBooleanVector(array);
                B.setAttr(attr);
                return B;
            }
            case 2: {
                UUID[] array = new UUID[n];
                for (; i < n; i++)
                    array[i] = rg();
                K.KGuidVector B = new K.KGuidVector(array);
                B.setAttr(attr);
                return B;
            }
            case 4: {
                byte[] array = new byte[n];
                for (; i < n; i++)
                    array[i] = b[j++];
                K.KByteVector G = new K.KByteVector(array);
                G.setAttr(attr);
                return G;
            }
            case 5: {
                short[] array = new short[n];
                for (; i < n; i++)
                    array[i] = rh();
                K.KShortVector H = new K.KShortVector(array);
                H.setAttr(attr);
                return H;
            }
            case 6: {
                int[] array = new int[n];
                for (; i < n; i++)
                    array[i] = ri();
                K.KIntVector I = new K.KIntVector(array);
                I.setAttr(attr);
                return I;
            }
            case 7: {
                long[] array = new long[n];
                for (; i < n; i++)
                    array[i] = rj();
                K.KLongVector J = new K.KLongVector(array);
                J.setAttr(attr);
                return J;
            }
            case 8: {
                float[] array = new float[n];
                for (; i < n; i++)
                    array[i] = re();
                K.KFloatVector E = new K.KFloatVector(array);
                E.setAttr(attr);
                return E;
            }
            case 9: {
                double[] array = new double[n];
                for (; i < n; i++)
                    array[i] = rf();
                K.KDoubleVector F = new K.KDoubleVector(array);
                F.setAttr(attr);
                return F;
            }
            case 10: {
                byte[] array = new byte[n];
                System.arraycopy(b, j, array, 0, n);
                K.KString C = new K.KString(array);
                C.setAttr(attr);
                j += n;
                return C;
            }
            case 11: {
                K.KSymbol[] array = new K.KSymbol[n];
                for (; i < n; i++)
                    array[i] = rs();
                K.KSymbolVector S = new K.KSymbolVector(array);
                S.setAttr(attr);
                return S;
            }
            case 12: {
                long[] array = new long[n];
                for (; i < n; i++) {
                    array[i] = rj();
                }
                K.KTimestampVector P = new K.KTimestampVector(array);
                P.setAttr(attr);
                return P;
            }
            case 13: {
                int[] array = new int[n];
                for (; i < n; i++)
                    array[i] = ri();
                K.KMonthVector M = new K.KMonthVector(array);
                M.setAttr(attr);
                return M;
            }
            case 14: {
                int[] array = new int[n];
                for (; i < n; i++)
                    array[i] = ri();
                K.KDateVector D = new K.KDateVector(array);
                D.setAttr(attr);
                return D;
            }
            case 15: {
                double[] array = new double[n];
                for (; i < n; i++)
                    array[i] = rf();
                K.KDatetimeVector Z = new K.KDatetimeVector(array);
                Z.setAttr(attr);
                return Z;
            }
            case 16: {
                long[] array = new long[n];
                for (; i < n; i++) {
                    array[i] = rj();
                }
                K.KTimespanVector N = new K.KTimespanVector(array);
                N.setAttr(attr);
                return N;
            }
            case 17: {
                int[] array = new int[n];
                for (; i < n; i++)
                    array[i] = ri();
                K.KMinuteVector U = new K.KMinuteVector(array);
                U.setAttr(attr);
                return U;
            }
            case 18: {
                int[] array = new int[n];
                for (; i < n; i++)
                    array[i] = ri();
                K.KSecondVector V = new K.KSecondVector(array);
                V.setAttr(attr);
                return V;
            }
            case 19: {
                int[] array = new int[n];
                for (; i < n; i++)
                    array[i] = ri();
                K.KTimeVector T = new K.KTimeVector(array);
                T.setAttr(attr);
                return T;
            }
        }
        return null;
    }

    boolean rb() {
        return 1 == b[j++];
    }

    short rh() {
        int x = b[j++], y = b[j++];
        return (short) (a ? x & 0xff | y << 8 : x << 8 | y & 0xff);
    }

    int ri() {
        int x = rh(), y = rh();
        return a ? x & 0xffff | y << 16 : x << 16 | y & 0xffff;
    }

    long rj() {
        int x = ri(), y = ri();
        return a ? x & 0xffffffffL | (long) y << 32 : (long) x << 32 | y & 0xffffffffL;
    }

    float re() {
        return Float.intBitsToFloat(ri());
    }

    double rf() {
        return Double.longBitsToDouble(rj());
    }

    UUID rg() {
        boolean oa = a;
        a = false;
        UUID g = new UUID(rj(), rj());
        a = oa;
        return g;
    }

    char rc() {
        return (char) (b[j++] & 0xff);
    }

    K.KSymbol rs() throws UnsupportedEncodingException {
        int n = j;
        for (; b[n] != 0; )
            ++n;
        byte[] array = new byte[n-j];
        System.arraycopy(b, j, array, 0, n-j);
        j = n;
        ++j;
        return new K.KSymbol(array);
    }

    K.UnaryPrimitive rup() {
        return new K.UnaryPrimitive(b[j++]);
    }

    K.BinaryPrimitive rbp() {
        return new K.BinaryPrimitive(b[j++]);
    }

    K.TernaryOperator rternary() {
        return new K.TernaryOperator(b[j++]);
    }

    K.Function rfn() throws UnsupportedEncodingException {
        K.KSymbol s = rs();
        return new K.Function((K.KString) r());
    }

    K.Feach rfeach() throws UnsupportedEncodingException {
        return new K.Feach(r());
    }

    K.Fover rfover() throws UnsupportedEncodingException {
        return new K.Fover(r());
    }

    K.Fscan rfscan() throws UnsupportedEncodingException {
        return new K.Fscan(r());
    }

    K.FComposition rcomposition() throws UnsupportedEncodingException {
        int n = ri();
        K.KBase[] objs = new K.KBase[n];
        for (int i = 0; i < n; i++)
            objs[i] = r();

        return new K.FComposition(objs);
    }

    K.FPrior rfPrior() throws UnsupportedEncodingException {
        return new K.FPrior(r());
    }

    K.FEachRight rfEachRight() throws UnsupportedEncodingException {
        return new K.FEachRight(r());
    }

    K.FEachLeft rfEachLeft() throws UnsupportedEncodingException {
        return new K.FEachLeft(r());
    }

    K.Projection rproj() throws UnsupportedEncodingException {
        int n = ri();
        K.KBase[] array = new K.KBase[n];
        for (int i = 0; i < n; i++)
            array[i] = r();
        return new K.Projection(array);
    }

    K.KMinute ru() {
        return new K.KMinute(ri());
    }

    K.KMonth rm() {
        return new K.KMonth(ri());
    }

    K.KSecond rv() {
        return new K.KSecond(ri());
    }

    K.KTimespan rn() {
        return new K.KTimespan(rj());
    }

    K.KTime rt() {
        return new K.KTime(ri());
    }

    K.KDate rd() {
        return new K.KDate(ri());
    }

    K.KDatetime rz() {
        return new K.KDatetime(rf());
    }

    K.KTimestamp rp() {
        return new K.KTimestamp(rj());
    }

}
