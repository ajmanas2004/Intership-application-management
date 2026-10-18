package com.internshipmanagement.view;

import com.internshipmanagement.entity.Company;
import javax.swing.JComboBox;
import java.util.List;
import java.util.Objects;

/** Shared company filter: preserve the selected ID while async options are refreshed. */
public class CompanySelector extends JComboBox<Company> {
    private boolean updating;
    private Runnable changed = () -> {};

    public CompanySelector() {
        Company all = company(null, "Tất cả công ty");
        addItem(all); setPrototypeDisplayValue(company(null, "Tất cả công ty / Chọn công ty"));
        setToolTipText("Lọc theo công ty");
        addActionListener(e -> { if (!updating) changed.run(); });
    }

    public void onChange(Runnable action) { changed = action; }
    public Long selectedId() { return getSelectedItem() instanceof Company c ? c.getId() : null; }
    public String selectedName() { return getSelectedItem() instanceof Company c ? c.getName() : ""; }

    public void select(Long id, String name) {
        updating = true;
        try {
            for (int i = 0; i < getItemCount(); i++) {
                if (Objects.equals(id, getItemAt(i).getId())) { setSelectedIndex(i); return; }
            }
            Company c = company(id, name == null ? "Công ty #" + id : name);
            addItem(c); setSelectedItem(c);
        } finally { updating = false; }
    }

    /** Returns true when the previously selected company no longer exists. */
    public boolean updateOptions(List<Company> companies) {
        Long id = selectedId();
        boolean missing = id != null && companies.stream().noneMatch(c -> id.equals(c.getId()));
        updating = true;
        try {
            removeAllItems(); addItem(company(null, "Tất cả công ty"));
            for (Company c : companies) {
                addItem(c); if (Objects.equals(id, c.getId())) setSelectedItem(c);
            }
        } finally { updating = false; }
        return missing;
    }

    private static Company company(Long id, String name) {
        Company c = new Company(); c.setId(id); c.setName(name); return c;
    }
}
