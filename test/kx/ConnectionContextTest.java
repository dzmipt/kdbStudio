package kx;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ConnectionContextTest {

    @Test
    public void testGetCertificate() {
        ConnectionContext context = new ConnectionContext();
        assertNull(context.getCertificate());

        context.setCertChain(null);
        assertNull(context.getCertificate());

        context.setCertChain(new X509Certificate[0]);
        assertNull(context.getCertificate());

        MockCertificate c1 = new MockCertificate();
        MockCertificate c2 = new MockCertificate();

        context.setCertChain(new X509Certificate[]{c1});
        assertSame(context.getCertificate(), c1);

        context.setCertChain(new X509Certificate[]{c2, c1});
        assertSame(context.getCertificate(), c2);
    }

    static class MockCertificate extends X509Certificate {
        @Override
        public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
        }

        @Override
        public int getVersion() {
            return 123;
        }

        @Override
        public BigInteger getSerialNumber() {
            return new BigInteger(new byte[] {10,11,12,13,14,15,16,17,18,19,20});
        }

        @Override
        public Principal getIssuerDN() {
            return new MockPrinciple();
        }

        @Override
        public Principal getSubjectDN() {
            return new MockPrinciple();
        }

        @Override
        public Date getNotBefore() {
            return new Date();
        }

        @Override
        public Date getNotAfter() {
            return new Date();
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return new byte[] {10,11,12,13,14,15,16,17,18,19,20};
        }

        @Override
        public byte[] getSignature() {
            return new byte[] {10,11,12,13,14,15,16,17,18,19,20};
        }

        @Override
        public String getSigAlgName() {
            return "MockSigAlgName";
        }

        @Override
        public String getSigAlgOID() {
            return "";
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getKeyUsage() {
            return new boolean[0];
        }

        @Override
        public int getBasicConstraints() {
            return 0;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return new byte[] {10,11,12,13,14,15,16,17,18,19,20};
        }

        @Override
        public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

        }

        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return Set.of();
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return Set.of();
        }

        @Override
        public byte[] getExtensionValue(String oid) {
            return new byte[0];
        }
    }

    static class MockPrinciple implements Principal{

        @Override
        public String getName() {
            return "CN=mock principle";
        }
    }

    static class MockPublicKey implements PublicKey {
        @Override
        public String getAlgorithm() {
            return "mockAlgorithm";
        }

        @Override
        public String getFormat() {
            return "mockFormat";
        }

        @Override
        public byte[] getEncoded() {
            return new byte[] {10,11,12,13,14,15,16,17,18,19,20};
        }
    }
}
