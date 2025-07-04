package studio.utils;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class ProxyTrustManager implements X509TrustManager {

    private final List<X509TrustManager> trustManagers = new ArrayList<>();

    public ProxyTrustManager(X509TrustManager... trustManagers) {
        this.trustManagers.addAll(List.of(trustManagers));
    }

    public void addTrustManager(X509TrustManager trustManager) {
        trustManagers.add(trustManager);
    }

    public int getCount() {
        return trustManagers.size();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        CertificateException proxyException = null;
        for (X509TrustManager trustManager: trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
                proxyException = buildException(proxyException, e);
            }
        }
        if (proxyException != null) throw proxyException;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        CertificateException proxyException = null;
        for (X509TrustManager trustManager: trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
                proxyException = buildException(proxyException, e);
            }
        }
        if (proxyException != null) throw proxyException;

    }

    private CertificateException buildException(CertificateException proxy, CertificateException exception) {
        if (proxy == null) return exception;
        return new CertificateException(proxy.getMessage() + "\n" + exception.getMessage(), exception);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> list = new ArrayList<>();
        for (X509TrustManager trustManager: trustManagers) {
            list.addAll(List.of(trustManager.getAcceptedIssuers()));
        }
        return list.toArray(new X509Certificate[0]);
    }
}
