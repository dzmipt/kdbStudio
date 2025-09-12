package studio.ui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class StringComboBox extends JComboBox<String> {

    private final Model model;

    public StringComboBox() {
        super(new Model());
        this.model = (Model) getModel();
    }

    public StringComboBox(Collection<String> items) {
        this();
        model.setItems(items);
    }

    public void setItems(Collection<String> items) {
        model.setItems(items);
    }

    public String getSelectedItem() {
        return model.selected;
    }

    @Override
    public void setSelectedItem(Object anObject) {
        model.setSelectedItem(anObject);
    }

    private static class Model extends AbstractListModel<String> implements ComboBoxModel<String> {

        private final List<String> items = new ArrayList<>();
        private String selected = null;

        public void setItems(Collection<String> newItems) {
            if (items == newItems) return;

            items.clear();
            items.addAll(newItems);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if (Objects.equals(selected, anItem)) return;

            this.selected = (String) anItem;
            fireContentsChanged(this, -1, -1);
        }

        @Override
        public Object getSelectedItem() {
            return selected;
        }

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public String getElementAt(int index) {
            return items.get(index);
        }
    }

}
