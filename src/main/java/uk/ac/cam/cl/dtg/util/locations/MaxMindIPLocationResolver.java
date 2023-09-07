package uk.ac.cam.cl.dtg.util.locations;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.Subdivision;
import org.apache.commons.lang3.Validate;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class MaxMindIPLocationResolver implements IPLocationResolver {
    private final DatabaseReader databaseReader;

    /**
     *  Create new IP location resolver using MaxMind City DB file.
     *
     *  Will throw an exception (NPE or IOException) if the file path is empty or invalid.
     *
     * @param properties to extract the database file location from
     * @throws IOException if the database file is absent.
     */
    public MaxMindIPLocationResolver(final AbstractConfigLoader properties) throws IOException {
        String databaseLocation = properties.getProperty(MAXMIND_CITY_DB_LOCATION);
        Validate.notBlank(databaseLocation);
        File database = new File(databaseLocation);
        databaseReader = new DatabaseReader.Builder(database).withCache(new CHMCache()).build();
    }

    @Override
    public Location resolveAllLocationInformation(final String ipAddress) throws IOException, LocationServerException {
        InetAddress inetAddress = InetAddress.getByName(ipAddress);

        try {
            CityResponse response = databaseReader.city(inetAddress);
            return responseToLocation(response);
        } catch (GeoIp2Exception e) {
            throw new LocationServerException(e.getMessage());
        }
    }

    @Override
    public Location resolveCountryOnly(final String ipAddress) throws IOException, LocationServerException {
        // It is no additional work to do just load everything.
        // FIXME; can we just remove this method from the interface?
        return resolveAllLocationInformation(ipAddress);
    }

    private Location responseToLocation(final CityResponse response) {

        Country country = response.getCountry();
        Subdivision county = response.getMostSpecificSubdivision();
        City city = response.getCity();
        Postal postal = response.getPostal();
        com.maxmind.geoip2.record.Location maxmindLocation = response.getLocation();

        Address partialAddress = new Address(null, null,
                null != city ? city.getName() : null,
                null != county ? county.getName() : null,
                null != postal ? postal.getCode() : null,
                null != country ? country.getName() : null
        );

        Double lat = null != maxmindLocation ? maxmindLocation.getLatitude() : null;
        Double lon = null != maxmindLocation ? maxmindLocation.getLongitude() : null;

        return new Location(partialAddress, lat, lon);
    }

}
