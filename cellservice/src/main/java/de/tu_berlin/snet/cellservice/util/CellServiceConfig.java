package de.tu_berlin.snet.cellservice.util;

/**
 * Created by Friedhelm Victor on 5/4/16.
 */
public class CellServiceConfig {
    private static CellServiceConfig instance;

    private int phoneNumberAnonymizationTechnique;
    // minimum time interval between location updates, in milliseconds
    private long minimumLocationUpdateIntervalTime;
    private long maximumGPSAge;
    private long maximumNetworkLocationAge;


    public static CellServiceConfig getInstance() {
        if (instance != null) return instance;
        else {
            CellServiceConfig config = new CellServiceConfig();
            config.setPhoneNumberAnonymizationTechnique(Anonymizer.RANDOMIZE);
            config.setMinimumLocationUpdateIntervalTime(6 * 1000); // as Google recommends?!
            config.setMaximumGPSAge(120 * 1000); // 2 minutes
            config.setMaximumNetworkLocationAge(0); // should always be fresh
            return config;
        }
    }

    public long getMaximumNetworkLocationAge() {
        return maximumNetworkLocationAge;
    }

    public void setMaximumNetworkLocationAge(long maximumNetworkLocationAge) {
        this.maximumNetworkLocationAge = maximumNetworkLocationAge;
    }

    public long getMaximumGPSAge() {
        return maximumGPSAge;
    }

    public void setMaximumGPSAge(long maximumGPSAge) {
        this.maximumGPSAge = maximumGPSAge;
    }

    public long getMinimumLocationUpdateIntervalTime() {
        return minimumLocationUpdateIntervalTime;
    }

    public void setMinimumLocationUpdateIntervalTime(long minimumLocationUpdateIntervalTime) {
        this.minimumLocationUpdateIntervalTime = minimumLocationUpdateIntervalTime;
    }

    public int getPhoneNumberAnonymizationTechnique() {
        return phoneNumberAnonymizationTechnique;
    }

    public void setPhoneNumberAnonymizationTechnique(int phoneNumberAnonymizationTechnique) {
        this.phoneNumberAnonymizationTechnique = phoneNumberAnonymizationTechnique;
    }
}
