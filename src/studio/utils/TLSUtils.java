package studio.utils;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;

public class TLSUtils {

    public static Collection<X509Certificate> loadX509Chain(Path path) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        try (InputStream in = Files.newInputStream(path)) {
            /* generateCertificates() will happily accept
               PEM or DER and returns *all* certs it finds */
            @SuppressWarnings("unchecked")
            Collection<X509Certificate> certs =
                    (Collection<X509Certificate>) cf.generateCertificates(in);
            return certs;
        }
    }

    public static X509Certificate loadX509(Path path) throws CertificateException, IOException {
        Optional<X509Certificate> result = loadX509Chain(path).stream().findFirst();
        if (result.isPresent()) return result.get();

        throw new IOException("No certificate found");
    }

    public static String convertToPem(X509Certificate cert) throws CertificateEncodingException {
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN CERTIFICATE-----\n");

        // Encode to Base64 with line wrapping at 64 characters
        byte[] encoded = cert.getEncoded();
        String base64Encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        pemBuilder.append(base64Encoded).append("\n");

        pemBuilder.append("-----END CERTIFICATE-----\n");
        return pemBuilder.toString();
    }

    public static String convertToPem(X509Certificate[] chain) throws CertificateEncodingException {
        StringBuilder builder = new StringBuilder();
        for(X509Certificate certificate: chain) {
            if (builder.length() > 0) builder.append("\n");
            String cn = getAttribute(certificate.getSubjectDN(), "CN");
            if (cn != null) builder.append(cn).append("\n");
            builder.append(convertToPem(certificate));
        }
        return builder.toString();
    }

    public static String getAttribute(Principal principal, String attribute) {
        try {
            LdapName ldapDN = new LdapName(principal.getName());
            for (Rdn rdn : ldapDN.getRdns()) {
                if (attribute.equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (InvalidNameException e) {}
        return null;
    }

    private static X509TrustManager getTrustManager(KeyStore ks) throws GeneralSecurityException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        return (X509TrustManager) Arrays.stream(tmf.getTrustManagers())
                .filter(t -> t instanceof X509TrustManager)
                .findFirst()
                .orElseThrow(() -> new GeneralSecurityException("No X509TrustManager found"));
    }

    public static X509TrustManager getDefaultTrustManager() throws GeneralSecurityException{
        return getTrustManager((KeyStore)null);
    }

    public static X509TrustManager trustManagerFromJKS(Path path) throws GeneralSecurityException, IOException {
        return trustManagerFromJKS(path, null);
    }

    public static X509TrustManager trustManagerFromJKS(Path path, String password) throws IOException, GeneralSecurityException {
        return getTrustManager(getKeyStore(path, password));
    }

    public static X509TrustManager getTrustManager(X509Certificate root) throws GeneralSecurityException {
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("rootCA", root);
            return getTrustManager(ks);
        } catch (IOException e) {
            throw new GeneralSecurityException("Unexpected IOException: " + e.getMessage(), e);
        }
    }

    private static KeyStore getKeyStore(Path path, String password) throws IOException, GeneralSecurityException {
        char[] pass = password == null ?  new char[0] : password.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                ks.load(in, pass);
            }
        } else {
            ks.load(null, pass);
        }
        return ks;

    }

    public static void addCAToJKS(Path path, X509Certificate root) throws IOException, GeneralSecurityException {
        KeyStore ks = getKeyStore(path, null);
        String cn = getAttribute(root.getSubjectDN(), "CN");
        if (cn == null) cn = "rootCA";
        String alias = cn;
        for (int index = 1; ks.isCertificateEntry(alias); index++ ) {
            alias = cn + "." + index;
        }

        ks.setCertificateEntry(alias, root);
        try (OutputStream out = Files.newOutputStream(path)) {
            ks.store(out, new char[0]);
        }
    }

}
