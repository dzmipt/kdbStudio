package studio.ui.tls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.ui.EscapeDialog;
import studio.ui.StudioOptionPane;
import studio.ui.Util;
import studio.utils.TLSUtils;

import javax.swing.*;
import java.awt.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class CertChainInfoDialog extends EscapeDialog {

    private final CertificateChainPanel chainPanel;
    private final Logger log = LogManager.getLogger();

    private Mode result = Mode.Info;

    public enum Mode {Info, AcceptOnly, AcceptAndStore};

    public CertChainInfoDialog(Component owner, X509Certificate[] certificates) {
        this(owner, certificates, Mode.Info);
    }

    public CertChainInfoDialog(Component owner, X509Certificate[] certificates, Mode mode) {
        super(owner, "Certificate Chain");
        chainPanel = new CertificateChainPanel(certificates);
        initComponents(owner, mode);
    }

    public CertChainInfoDialog(Component owner, X509Certificate certificate) {
        super(owner, "Certificate");
        chainPanel = new CertificateChainPanel(certificate);
        initComponents(owner, Mode.Info);
    }

    public Mode getModeResult() {
        return result;
    }

    private void initComponents(Component owner, Mode mode) {
        JButton btnCopyCert = new JButton("Copy as PEM");
        btnCopyCert.addActionListener(
                e-> copy(chainPanel.getSelectedCertificate())
        );

        JButton btnCopyChain = new JButton("Copy chain as PEM");
        btnCopyChain.addActionListener(
                e-> copy(chainPanel.getChain())
        );

        JButton btnAccept = new JButton("Accept now");
        btnAccept.setToolTipText("Accept only for current session");
        btnAccept.addActionListener(
                e -> {
                    result = Mode.AcceptOnly;
                    accept();
                }
        );

        JButton btnAcceptAndStore = new JButton("Persist");
        btnAcceptAndStore.setToolTipText("Accept for current session and persist the top certificate to the trust store");
        btnAcceptAndStore.addActionListener(
                e -> {
                    result = Mode.AcceptAndStore;
                    accept();
                }
        );
        btnAcceptAndStore.setEnabled(mode == Mode.AcceptAndStore);


        JButton btnClose = new JButton(mode == Mode.Info ? "Close" : "Reject");
        btnClose.addActionListener(
                e -> cancel()
        );

        Box boxButtons = Box.createHorizontalBox();
        boxButtons.add(Box.createHorizontalGlue());
        boxButtons.add(btnCopyCert);
        boxButtons.add(Box.createHorizontalGlue());
        boxButtons.add(btnCopyChain);

        if (mode == Mode.AcceptOnly || mode == Mode.AcceptAndStore) {
            boxButtons.add(Box.createHorizontalGlue());
            boxButtons.add(btnAccept);
            boxButtons.add(Box.createHorizontalGlue());
            boxButtons.add(btnAcceptAndStore);
        }

        boxButtons.add(Box.createHorizontalGlue());
        boxButtons.add(btnClose);
        boxButtons.add(Box.createHorizontalGlue());

        boxButtons.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel content = new JPanel(new BorderLayout());
        content.add(chainPanel, BorderLayout.CENTER);
        content.add(boxButtons, BorderLayout.SOUTH);

        setContentPane(content);

        alignAndShow();
    }

    private void copy(X509Certificate certificate) {
        try {
            Util.copyTextToClipboard(TLSUtils.convertToPem(certificate));
        } catch (CertificateEncodingException e) {
            log.error("Error during certificate conversion", e);
            StudioOptionPane.showError(this,"Error", e.getMessage());
        }
    }

    private void copy(X509Certificate[] chain) {
        try {
            Util.copyTextToClipboard(TLSUtils.convertToPem(chain));
        } catch (CertificateEncodingException e) {
            log.error("Error during certificate chain conversion", e);
            StudioOptionPane.showError(this,"Error", e.getMessage());
        }
    }
}
