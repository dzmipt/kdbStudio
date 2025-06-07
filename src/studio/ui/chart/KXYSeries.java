package studio.ui.chart;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import studio.kdb.K;
import studio.kdb.KColumn;
import studio.kdb.ToDouble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KXYSeries extends XYSeries {

    private final List<XYDataItem> data;
    private final double minX, maxX, minY, maxY;

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public List<XYDataItem> getItems() {
        return Collections.unmodifiableList(data);
    }

    @Override
    public double getMinX() {
        return minX;
    }

    @Override
    public double getMaxX() {
        return maxX;
    }

    @Override
    public double getMinY() {
        return minY;
    }

    @Override
    public double getMaxY() {
        return maxY;
    }

    @Override
    public Number getX(int index) {
        return data.get(index).getX();
    }

    @Override
    public Number getY(int index) {
        return data.get(index).getY();
    }

    @Override
    public void add(XYDataItem item, boolean notify) {
        throw new IllegalStateException("KXYSeries modification is not supported");
    }

    public KXYSeries(KColumn xColumn, KColumn yColumn) {
        super(yColumn.getName(),true, false);

        double aminY = Double.MAX_VALUE;
        double amaxY = Double.MIN_VALUE;
        data = new ArrayList<>();
        for (int row = 0; row < xColumn.size(); row++) {
            K.KBase xValue = xColumn.get(row);
            K.KBase yValue = yColumn.get(row);
            if (xValue.isNull() || yValue.isNull()) continue;

            ToDouble x = (ToDouble)xValue;
            ToDouble y = (ToDouble)yValue;
            if (x.isInfinity() || y.isInfinity()) continue;

            double xDouble = x.toDouble();
            double yDouble = y.toDouble();
            aminY = Math.min(aminY, yDouble);
            amaxY = Math.max(amaxY, yDouble);
            data.add(new XYDataItem(xDouble, yDouble));
            data.sort( (o1, o2) -> {
                double d = o1.getXValue() - o2.getXValue();
                if (d<0) return -1;
                if (d>0) return 1;
                return 0;
            } );
        }

        minY = aminY;
        maxY = amaxY;
        minX = data.get(0).getXValue();
        maxX = data.get( data.size() - 1 ).getXValue();
    }
}
