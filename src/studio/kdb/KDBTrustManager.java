package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.ui.EscapeDialog;
import studio.ui.StudioOptionPane;
import studio.ui.StudioWindow;
import studio.ui.tls.CertChainInfoDialog;
import studio.utils.ProxyTrustManager;
import studio.utils.TLSUtils;
import studio.utils.log4j.EnvConfig;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class KDBTrustManager implements X509TrustManager {

    private final static Logger log = LogManager.getLogger();
    private final static String FILENAME = "cacerts";

    private final StudioWindow studioWindow;

    private boolean reconnect = true;
    private X509Certificate acceptCertificate;
    private X509Certificate[] chain;

    private static ProxyTrustManager instance;

    static {
        reloadInstance();
    }

    private static synchronized void reloadInstance() {
        instance = new ProxyTrustManager();
        try {
            instance.addTrustManager(TLSUtils.getDefaultTrustManager());
        } catch (GeneralSecurityException e) {
            log.error("Can't get default TrustManager", e);
        }

        Path path = EnvConfig.getFilepath(FILENAME);
        try {
            if (Files.exists(path)) {
                instance.addTrustManager(TLSUtils.trustManagerFromJKS(path));
            }
        } catch (IOException|GeneralSecurityException e) {
            log.error("Error load trust store from environment: {}", path, e);
        }

        path = EnvConfig.getPluginFolder().resolve(FILENAME);
        try {
            if (Files.exists(path)) {
                instance.addTrustManager(TLSUtils.trustManagerFromJKS(path));
            }
        } catch (IOException|GeneralSecurityException e) {
            log.error("Error load trust store from plugin folder: {}", path, e);
        }
    }

    public KDBTrustManager(StudioWindow studioWindow) {
        this.studioWindow = studioWindow;
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    public X509Certificate[] getCertificateChain() {
        return chain;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new CertificateException("checkClintTrusted is not expected");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        this.chain = chain;
        if (acceptCertificate != null && acceptCertificate.equals(chain[0])) return;

        CertificateException certException;
        try {
            instance.checkServerTrusted(chain, authType);
            return;
        } catch (CertificateException e) {
            certException = e;
            log.warn("Got exception on validation server certificate: {}", certException.getMessage());
        }

        boolean canStore = false;
        try {
            new ProxyTrustManager(instance, TLSUtils.getTrustManager(chain[chain.length - 1]));
            canStore = true;
        } catch (GeneralSecurityException ignored) { }

        CertChainInfoDialog dialog = new CertChainInfoDialog(studioWindow, chain,
                canStore ? CertChainInfoDialog.Mode.AcceptAndStore : CertChainInfoDialog.Mode.AcceptOnly);

        if (dialog.getResult() == EscapeDialog.DialogResult.CANCELLED) {
            setReconnect(false);
            throw certException;
        }

        if (dialog.getModeResult() == CertChainInfoDialog.Mode.AcceptOnly) {
            this.acceptCertificate = chain[0];
            setReconnect(true);
        } else {
            try {
                log.info("Adding new root to the trust store");
                TLSUtils.addCAToJKS(EnvConfig.getFilepath(FILENAME), chain[chain.length - 1]);
                reloadInstance();
            } catch (IOException | GeneralSecurityException e) {
                log.error("Error during trust store update", e);
                StudioOptionPane.showError(studioWindow,
                        "Error during trust store update: " + e.getMessage() + ". Will accept without adding to trust store",
                        "Can't save trust store");
                this.acceptCertificate = chain[0];
            }
            setReconnect(true);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return instance.getAcceptedIssuers();
    }
}
