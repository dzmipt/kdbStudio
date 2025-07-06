package studio.ui.tls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.ui.GroupLayoutSimple;
import studio.ui.StudioOptionPane;
import studio.ui.Util;
import studio.utils.TLSUtils;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

public class CertificateChainPanel extends JPanel {

    private final static Logger log = LogManager.getLogger();

    private final static DateTimeFormatter dateFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.LONG)
            .withLocale(Locale.getDefault());

    private final X509Certificate[] certificates;

    private JComponent pnlChain;
    private JTree tree;
    private JTextField txtSubject, txtIssuer, txtFrom, txtUntil, txtSerial, txtPublicKey, txtCertificate;

    public CertificateChainPanel(X509Certificate[] certificates) {
        this.certificates = certificates;
        initComponents();
    }

    private void initComponents() {
        txtSubject = getTextField();
        txtIssuer = getTextField();
        txtFrom = getTextField();
        txtUntil = getTextField();
        txtSerial = getTextField();
        txtPublicKey = getTextField();
        txtCertificate = getTextField();

        pnlChain = new JPanel(new BorderLayout());
        tree = getTree();
        pnlChain.add(tree, BorderLayout.CENTER);
        pnlChain.add(getButtonPanel(), BorderLayout.EAST);

        JLabel lblChain = getLabel("Certificate Chain:");


        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(
                GroupLayoutSimple.maxWidthStack()
                        .addLine(lblChain)
                        .addLine(getLabel("Subject:"))
                        .addLine(getLabel("Issuer:"))
                        .addLine(getLabel("Valid From:"))
                        .addLine(getLabel("Valid Until:"))
                        .addLine(getLabel("Serial Number:"))
                        .addLine(getLabel("Public Key SHA-256:"))
                        .addLine(getLabel("Certificate SHA-256:")),
                GroupLayoutSimple.maxWidthStack()
                        .addLine(pnlChain)
                        .addLine(txtSubject)
                        .addLine(txtIssuer)
                        .addLine(txtFrom)
                        .addLine(txtUntil)
                        .addLine(txtSerial)
                        .addLine(txtPublicKey)
                        .addLine(txtCertificate)
        );

        select(certificates[0]);
    }

    private JTree getTree() {
        JTree tree = new JTree(new Node(certificates.length-1, null));

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        tree.setCellRenderer(renderer);

        for (int i=0; i< certificates.length; i++) {
            tree.expandRow(i);
        }

        tree.setSelectionRow(certificates.length-1);
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                throw new ExpandVetoException(event);
            }
        });


        tree.addTreeSelectionListener(e -> select(getSelectedCertificate()) );

        tree.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        tree.setBorder(UIManager.getBorder("TextField.border"));
        return tree;
    }

    private Component getButtonPanel() {
        JButton btnCopyCert = new JButton("Copy selected");
        btnCopyCert.addActionListener(
                e-> copy(getSelectedCertificate())
        );

        JButton btnCopyChain = new JButton("Copy chain");
        btnCopyChain.addActionListener(
                e-> copy(certificates)
        );

        Box boxButtons = Box.createVerticalBox();
        boxButtons.add(Box.createVerticalGlue());
        boxButtons.add(btnCopyChain);
        boxButtons.add(Box.createVerticalGlue());
        boxButtons.add(btnCopyCert);
        boxButtons.add(Box.createVerticalGlue());

        boxButtons.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        return boxButtons;
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

    private JLabel getLabel(String text) {
        JLabel label = new JLabel(text);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }
    private JTextField getTextField() {
        JTextField textField = new JTextField(52);
        textField.setEditable(false);
        return textField;
    }

    private void select(X509Certificate certificate) {
        setText(txtSubject, certificate.getSubjectDN().toString());
        setText(txtIssuer, certificate.getIssuerDN().toString());

        setText(txtFrom, formatDate(certificate.getNotBefore()));
        setText(txtUntil, formatDate(certificate.getNotAfter()));

        alert(txtFrom, ! certificate.getNotBefore().before(new Date()) );
        alert(txtUntil, ! certificate.getNotAfter().after(new Date()) );

        setText(txtSerial, getSerial(certificate));
        setText(txtPublicKey, sha256(certificate.getPublicKey().getEncoded()));
        setText(txtCertificate, sha256(certificate));
    }

    private void setText(JTextField txtField, String text) {
        txtField.setText(text);
        txtField.setCaretPosition(0);
    }

    private String getSerial(X509Certificate certificate) {
        byte[] raw = certificate.getSerialNumber().toByteArray();          // two-s complement, may start with 0x00
        if (raw.length > 1 && raw[0] == 0) {        // strip sign byte if present
            raw = java.util.Arrays.copyOfRange(raw, 1, raw.length);
        }
        return formatByteArray(raw);
    }

    private String formatDate(Date date) {
        return dateFormatter.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    private String formatByteArray(byte[] array) {
        StringBuilder sb = new StringBuilder(array.length * 2);
        for (byte b : array) {
            if (sb.length()>0) sb.append(':');
            sb.append(String.format("%02x", b));      // always two hex digits
        }
        return sb.toString();               // e.g. "003F1A9C42"
    }

    private String sha256(byte[] array) {
        try {
            return formatByteArray(MessageDigest.getInstance("SHA-256").digest(array));
        } catch (NoSuchAlgorithmException e) {
            return "<" + e.getMessage() + ">";
        }
    }

    private String sha256(X509Certificate certificate) {
        try {
            return sha256(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            return "<" + e.getMessage() + ">";
        }
    }

    private void alert(JTextField textField, boolean isAlert) {
        Font font = UIManager.getFont("TextField.font");
        Color color = UIManager.getColor("TextField.foreground");
        if (isAlert) {
            font = font.deriveFont(Font.BOLD);
            color = Color.red;
        }

        textField.setFont(font);
        textField.setForeground(color);
    }

    public X509Certificate getSelectedCertificate() {
        Node aNode = (Node) tree.getLastSelectedPathComponent();
        if (aNode == null) return null;

        return certificates[aNode.getIndex()];
    }

    public X509Certificate[] getChain() {
        return certificates;
    }

    private class Node implements TreeNode {

        private final int index;
        private final TreeNode parent, child;
        Node(int index, TreeNode parent) {
            this.index = index;
            this.parent = parent;
            this.child = index == 0 ? null : new Node(index-1, this);
        }

        public int getIndex() {
            return index;
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            return child;
        }

        @Override
        public int getChildCount() {
            return child == null ? 0 : 1;
        }

        @Override
        public TreeNode getParent() {
            return parent;
        }

        @Override
        public int getIndex(TreeNode node) {
            return child == node ? 0 : -1;
        }

        @Override
        public boolean getAllowsChildren() {
            return false;
        }

        @Override
        public boolean isLeaf() {
            return child == null;
        }

        @Override
        public Enumeration<? extends TreeNode> children() {
            return isLeaf() ?
                    Collections.emptyEnumeration() :
                    Collections.enumeration(Collections.singleton(child));
        }

        @Override
        public String toString() {
            String cn = TLSUtils.getAttribute(certificates[index].getSubjectDN(), "CN");
            if (cn == null ) cn = "<not defined>";
            return cn;
        }
    }

}
