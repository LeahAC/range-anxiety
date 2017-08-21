package com.rangeanxiety.app.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.rangeanxiety.app.api.Marker;
import com.rangeanxiety.app.api.Polygon;
import com.rangeanxiety.app.helper.Haversine;
import com.rangeanxiety.app.persistence.MemPersistence;
import com.rangeanxiety.app.persistence.Persistence;
import com.rangeanxiety.app.persistence.POsmHandler;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import de.topobyte.osm4j.core.model.impl.Node;

import java.io.*;
import java.util.*;


@Repository
public class Network {

    @Autowired
    Haversine hav;
    @Autowired
    Polygon polygon;
    @Autowired
    Marker marker;

    public Multimap<Long, Double> vertices = ArrayListMultimap.create();

    private long vertexKey;
    private int count, nodecount = 0;
    private PbfReader reader;
    private POsmHandler handler;
    private Persistence db;


    public void readOSMFile() throws FileNotFoundException {
        String filename = "Jordan.osm.pbf";

        reader = new PbfReader(filename, false);

        db = new MemPersistence();


        handler = new POsmHandler(db);
        reader.setHandler(handler);
        try {
            reader.read();
            nodecount = handler.numNodes;
            vertices = handler.ver;

        } catch (OsmInputException e) {
        }

    }


    public long[] getVertexId() {


        List<Long> keys = new ArrayList<Long>(vertices.keySet());
        long arr[] = new long[nodecount];
        int i = 0;

        String result = null;
        for (Long key : keys) {
            arr[i] = key;
            i++;
        }
        return arr;
    }

    public double getRandomLat() {

        double lat;
        Random random = new Random();
        List<Long> keys = new ArrayList<Long>(vertices.keySet());
        long randomKey = keys.get(random.nextInt(keys.size()));
        System.out.println("Random key " + randomKey);
        vertexKey = randomKey;
        Collection<Double> coor = vertices.get(vertexKey);
        lat = coor.iterator().next();
        System.out.println("Latitude " + lat);
        return lat;
    }


    public double getRandomLon() {

        double lat, lon;
        Collection<Double> coor = vertices.get(vertexKey);
        lat = coor.iterator().next();
        Iterator<Double> iter = coor.iterator();
        iter.next();
        lon = iter.next();
        System.out.println("Longitude " + lon);

        return lon;
    }

    public double getLat(long key) {
        double lat;
        Collection<Double> coor = vertices.get(key);
        lat = coor.iterator().next();

        return lat;
    }

    public double getLon(long key) {
        double lon, lat;
        Collection<Double> coor = vertices.get(key);
        lat = coor.iterator().next();
        Iterator<Double> iter = coor.iterator();
        iter.next();
        lon = iter.next();
        return lon;
    }

    //Call getnodes
    public String getNodes(double lat, double lng, double range, int choice)//range in miles

    {
        long arr[] = new long[nodecount];
        long key[] = new long[nodecount];
        String result = null;
        arr = getVertexId();

        double lat1, lon1;
        lat1 = lat;
        lon1 = lng;


        count = 0;
        double lat2, lon2;

        for (int i = 0; i < nodecount; i++) {
            lat2 = getLat(arr[i]);
            lon2 = getLon(arr[i]);
            double checkdis = hav.Havdistance(lat1, lon1, lat2, lon2);
            if ((checkdis > (range - 0.1)) && (checkdis < (range + 0.1))) {
                key[count] = arr[i];

                count++;
            }
        }


        result = arrangedCoordinate(key, choice);

        return result;
    }

    public String getNodes(long nodeId, double range, int choice)//range in Miles

    {
        Node startNode = db.getNodeById(nodeId);
        if (startNode == null) {
            return null;
        } else {
            long arr[] = new long[nodecount];
            long key[] = new long[nodecount];
            String result = null;
            arr = getVertexId();

            double lat1, lon1;
            lat1 = startNode.getLatitude();
            lon1 = startNode.getLongitude();


            count = 0;
            double lat2, lon2;

            for (int i = 0; i < nodecount; i++) {
                lat2 = getLat(arr[i]);
                lon2 = getLon(arr[i]);
                double checkdis = hav.Havdistance(lat1, lon1, lat2, lon2);
                if ((checkdis > (range - 0.1)) && (checkdis < (range + 0.1))) {
                    key[count] = arr[i];

                    count++;
                }
            }


            result = arrangedCoordinate(key, choice);

            return result;
        }
    }

    public String arrangedCoordinate(long arr[], int choice) {


        double DistanceMatrix[][] = new double[count][count];
        int i, j;
        String result = null;
        double lat1, lon1, lat2, lon2;
        for (i = 0; i < count; i++) {
            lat1 = getLat(arr[i]);
            lon1 = getLon(arr[i]);
            for (j = 0; j < count; j++) {
                if (i == j) {
                    DistanceMatrix[i][j] = 0d;
                    continue;
                } else {
                    lat2 = getLat(arr[j]);
                    lon2 = getLon(arr[j]);
                    DistanceMatrix[i][j] = hav.Havdistance(lat1, lon1, lat2, lon2);

                }
            }
        }
        //Call and pass DistanceMatrix to TravellingSalesman;

        try {
            result = travellingSalesman(DistanceMatrix, arr, choice);
        } catch (Exception e) {
            //do nothing
        }


        return result;
    }

    public String travellingSalesman(double DistanceMatrix[][], long arr[], int choice) throws Exception {
        Stack<Integer> stack = new Stack<Integer>();
        long newarr[] = new long[count];
        newarr[0] = arr[0];
        int[] visited = new int[count];
        for (int i = 0; i < count; i++) {
            visited[i] = 0;
        }
        int keycount = 1;
        String result = null;
        visited[0] = 1;
        stack.push(0);
        int element, dst = 0, i;
        double min = Integer.MAX_VALUE;
        boolean minFlag = false;

        while (!stack.isEmpty()) {
            element = stack.peek();
            i = 0;
            min = Integer.MAX_VALUE;
            while (i < count) {
                if (DistanceMatrix[element][i] > 0 && visited[i] == 0) {
                    if (min > DistanceMatrix[element][i]) {
                        min = DistanceMatrix[element][i];
                        dst = i;
                        minFlag = true;
                    }
                }
                i++;
            }
            if (minFlag) {
                visited[dst] = 1;

                newarr[keycount] = arr[dst];
                stack.push(dst);

                keycount++;
                minFlag = false;
                continue;
            }
            stack.pop();
        }


        int len = 1;
        long longArr[] = new long[count];
        int index = 0, c = 0;
        longArr[0] = newarr[0];
        double lat1, lon1, lat2, lon2;
        while ((len) < count) {
            lat1 = getLat(newarr[index]);
            lon1 = getLon(newarr[index]);
            lat2 = getLat(newarr[len]);
            lon2 = getLon(newarr[len]);
            double distance = hav.Havdistance(lat1, lon1, lat2, lon2);

            if (distance < 1) {
                len++;

            } else {
                longArr[++c] = newarr[len];
                index = len;
                len++;

            }
        }


        if (newarr == null) {
            return null;
        }
        switch (choice) {
            case 1:
                result = polygon.convertToJSONpolygon(longArr, c + 1);
                break;
            case 2:
                result = marker.convertToJSONmarker(longArr, c + 1);
                break;
            default:
                break;
        }

        return result;
    }


}
