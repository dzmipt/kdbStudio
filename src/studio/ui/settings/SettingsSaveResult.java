package studio.ui.settings;

public class SettingsSaveResult {

    private boolean refreshEditorsSettings = false;
    private boolean refreshComboServerVisibility = false;
    private boolean refreshResultSettings = false;
    private boolean changedLF = false;

    public boolean isRefreshEditorsSettings() {
        return refreshEditorsSettings;
    }

    public void setRefreshEditorsSettings(boolean refreshEditorsSettings) {
        this.refreshEditorsSettings |= refreshEditorsSettings;
    }

    public boolean isRefreshComboServerVisibility() {
        return refreshComboServerVisibility;
    }

    public void setRefreshComboServerVisibility(boolean refreshComboServerVisibility) {
        this.refreshComboServerVisibility |= refreshComboServerVisibility;
    }

    public boolean isRefreshResultSettings() {
        return refreshResultSettings;
    }

    public void setRefreshResultSettings(boolean refreshResultSettings) {
        this.refreshResultSettings |= refreshResultSettings;
    }

    public boolean isChangedLF() {
        return changedLF;
    }

    public void setChangedLF(boolean changedLF) {
        this.changedLF |= changedLF;
    }
}
