package studio.utils;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class BasicDataFlavor extends DataFlavor {

    private final Class<?> clazz;
    public BasicDataFlavor(Class<?> clazz) {
        super(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + clazz.getName() + "\"",
               clazz.getName() );

        this.clazz = clazz;
    }

    public Transferable getTransferable(Object object) {
        if (! clazz.isInstance(object))
            throw new IllegalArgumentException("Expected object of class " + clazz + "; passed " + object.getClass());


        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{BasicDataFlavor.this};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return BasicDataFlavor.this.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return object;
            }
        };
    }
}
