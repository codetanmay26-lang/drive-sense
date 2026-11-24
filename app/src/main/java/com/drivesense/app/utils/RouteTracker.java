package com.drivesense.app.utils;

import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * RouteTracker — accumulates GPS points, computes trip distance, max/avg speed.
 */
public class RouteTracker {

    private List<LatLng> points = new ArrayList<>();
    private double totalDistanceKm = 0.0;
    private float  maxSpeedKmh    = 0f;
    private float  speedSum       = 0f;
    private int    speedCount     = 0;
    private double lastLat = Double.NaN;
    private double lastLon = Double.NaN;

    public void addPoint(double lat, double lon) {
        if (!Double.isNaN(lastLat)) {
            double segmentKm = haversineKm(lastLat, lastLon, lat, lon);
            if (segmentKm < 0.005) return;
            totalDistanceKm += segmentKm;
        }
        lastLat = lat;
        lastLon = lon;
        points.add(new LatLng(lat, lon));
    }

    public void addSpeedSample(float speedKmh) {
        if (speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh;
        speedSum += speedKmh;
        speedCount++;
    }

    public double getTotalDistanceKm() { return totalDistanceKm; }
    public float  getMaxSpeedKmh()    { return maxSpeedKmh; }
    public float  getAvgSpeedKmh()    { return speedCount > 0 ? speedSum / speedCount : 0f; }
    public List<LatLng> getPoints()   { return points; }

    /**
     * Encode route as a JSON array of [lat, lon] pairs for storage.
     */
    public String getRouteJson() {
        JSONArray arr = new JSONArray();
        try {
            for (LatLng p : points) {
                JSONObject obj = new JSONObject();
                obj.put("lat", p.latitude);
                obj.put("lon", p.longitude);
                arr.put(obj);
            }
        } catch (JSONException e) { /* ignore */ }
        return arr.toString();
    }

    /**
     * Decode a stored route JSON back to LatLng list.
     */
    public static List<LatLng> decodeRouteJson(String json) {
        List<LatLng> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new LatLng(obj.getDouble("lat"), obj.getDouble("lon")));
            }
        } catch (JSONException e) { /* ignore */ }
        return result;
    }

    public void reset() {
        points.clear();
        totalDistanceKm = 0;
        maxSpeedKmh = 0;
        speedSum    = 0;
        speedCount  = 0;
        lastLat     = Double.NaN;
        lastLon     = Double.NaN;
    }

    /**
     * Haversine formula: distance in km between two GPS coordinates.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
