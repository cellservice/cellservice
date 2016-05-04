package de.tu_berlin.snet.cellservice.util;

/**
 * Created by Friedhelm Victor on 5/4/16.
 */
public class CellServiceConfig {
    private static CellServiceConfig instance;

    private int phoneNumberAnonymizationTechnique;

    public static CellServiceConfig getInstance() {
        if (instance == null) return instance;
        else {
            CellServiceConfig config = new CellServiceConfig();
            config.setPhoneNumberAnonymizationTechnique(Anonymizer.RANDOMIZE);
            return config;
        }
    }

    public int getPhoneNumberAnonymizationTechnique() {
        return phoneNumberAnonymizationTechnique;
    }

    public void setPhoneNumberAnonymizationTechnique(int phoneNumberAnonymizationTechnique) {
        this.phoneNumberAnonymizationTechnique = phoneNumberAnonymizationTechnique;
    }
}
