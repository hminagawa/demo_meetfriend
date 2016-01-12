/*
 * Copyright 2013 Team EGG. Co.ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appiaries.meetfriend.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.widget.Toast;

import com.appiaries.meetfriend.R;

public class AddressSearchUtils {

    /**
     * Listener for Geo position search.
     */
    public interface AddressSearchListener {
        /**
         * Called when the search is done.
         *
         * @param addressList
         *            List for {@link Address}
         */
        void onFinishSearch(final List<Address> addressList);
    }

    /**
     * Listener for specified coordinate Geo position search.
     */
    public interface SingleAddressSearchListener {
        /**
         * Called when the search is done.
         * 
         * @param address
         *            {@link Address}
         */
        void onFinishSearch(final Address address);

    }

    /**
     * Search for the Geo pos coordinate.
     *
     * @param context
     *            {@link Context}
     * @param placeName
     *            Position name
     * @param listener
     *            {@link AddressSearchListener}
     */
    public static void searchAddress(final Context context, final String placeName, final AddressSearchListener listener) {
        // Search for the Geo pos, and show the Geo pos registration dialog.
        new AsyncTask<Void, Void, List<Address>>() {
            @Override
            protected List<Address> doInBackground(Void... params) {
                // Obtaining the address.
                final Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
                final List<Address> addressListJp = new ArrayList<>();
                try {
                    List<Address> addressList = geoCoder.getFromLocationName(placeName, 100);
                    if (addressList != null) {
                        final int size = addressList.size();
                        for (int i = 0; i < size; i++) {
                            final Address address = addressList.get(i);
                            // Register JAPAN only.
                            if ("JP".equals(address.getCountryCode())) {
                                addressListJp.add(address);
                            }
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(context, R.string.error_location_service, Toast.LENGTH_SHORT).show();
                    return addressListJp;
                }

                return addressListJp;
            }

            @Override
            protected void onPostExecute(List<Address> addressList) {
                listener.onFinishSearch(addressList);
            }
        }.execute();
    }

    /**
     * Search for {@link Address} from specified coordinates.
     * 
     * @param context
     *            {@link Context}
     * @param longitude
     *            Longitude
     * @param latitude
     *            Latitude
     * @param listener
     *            {@link SingleAddressSearchListener}
     */
    public static void searchAddress(final Context context, final double longitude, final double latitude, final SingleAddressSearchListener listener) {
        new AsyncTask<Void, Void, Address>() {
            @Override
            protected Address doInBackground(Void... params) {
                return searchAddressSync(context, longitude, latitude);
            }

            @Override
            protected void onPostExecute(Address address) {
                listener.onFinishSearch(address);
            }
        }.execute();
    }

    /**
     * Search for {@link Address} synchronously from the specified coordinates.
     *
     * @param context
     *            {@link Context}
     * @param longitude
     *            Longitude
     * @param latitude
     *            Latitude
     * @return {@link Address}
     */
    public static Address searchAddressSync(final Context context, final double longitude, final double latitude) {
        // Obtaining the address.
        final Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addressList = geoCoder.getFromLocation(latitude, longitude, 1);
            if (addressList != null && !addressList.isEmpty()) {
                return addressList.get(0);
            }
        } catch (IOException e) {
            return null;
        }
        return null;

    }
}
