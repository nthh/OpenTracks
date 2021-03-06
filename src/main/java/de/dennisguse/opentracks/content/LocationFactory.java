package de.dennisguse.opentracks.content;

import android.location.Location;
import android.location.LocationManager;

import de.dennisguse.opentracks.content.data.TrackPoint;

/**
 * Creates a new {@link TrackPoint}.
 * An implementation can create new instances or reuse existing instances for optimization.
 */
public class LocationFactory {

    /**
     * The default {@link LocationFactory} which creates a location each time.
     */
    public static LocationFactory DEFAULT_LOCATION_FACTORY = new LocationFactory();

    public Location createLocation() {
        return new TrackPoint(LocationManager.GPS_PROVIDER);
    }
}
