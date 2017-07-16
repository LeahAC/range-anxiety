package com.rangeanxiety.app.service;

import java.io.*;
import java.lang.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;


import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.ArrayListMultimap;


import crosby.binary.osmosis.OsmosisReader;
import com.rangeanxiety.app.entities.Edge;
import com.rangeanxiety.app.entities.Vertex;


@Repository
public class Network {
    private Multimap<Long, Double> ver = ArrayListMultimap.create();
    
     
    private Map<Long, Vertex> vertices = new HashMap<>();

    private Multimap<Long, Edge> edges = MultimapBuilder.hashKeys().hashSetValues().build();


    public void initialize() throws Exception {
        readOSMFile("test.osm.pbf");
        // computeEdgeDistances();

    }

    private void readOSMFile(String filename) throws FileNotFoundException {
        // On booting up, load the data from file
        File testFile = new File(filename);
        FileInputStream fis = new FileInputStream(testFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        OsmosisReader reader = new OsmosisReader(bis);
        // The sink serves as a callback, reacting on any nodes and ways found
        reader.setSink(new Sink() {
        

            @Override
            public void initialize(Map<String, Object> arg0) {
                // do nothing
            }

            @Override
            public void process(EntityContainer entityContainer) {
                Entity entity = entityContainer.getEntity();
                switch (entity.getType()) {
                    case Node:
                        Node node = (Node) entity;
                        ver.put(node.getId(), node.getLatitude());
                        ver.put(node.getId(), node.getLongitude());


                        vertices.put(node.getId(), new Vertex(node.getLatitude(), node.getLongitude()));

                        break;
                    case Way:
                        Way way = (Way) entity;
                        List<WayNode> nodes = way.getWayNodes();
                       
                        for (int i = 1; i < nodes.size(); i++) {

                            long from = nodes.get(i - 1).getNodeId();
                             
                            long to = nodes.get(i).getNodeId();
                            
                            edges.put(from, new Edge(Double.POSITIVE_INFINITY, to));

                            edges.put(to, new Edge(Double.POSITIVE_INFINITY, from));
                            
                            
                        }

                        break;
                    default:
                        break;
                }

            }

            @Override
            public void complete() {
                // do nothing
            }

            @Override
            public void release() {
                // do nothing
            }

        });
        reader.run();
    }


    public long[] get50RandomVertexId() {
        Random random = new Random();
        List<Long> keys = new ArrayList<Long>(ver.keySet());
        long arr[] = new long[50];
        int i;
        System.out.println("To be converted to Json/GeoJson");
        for (i = 0; i < 50; i++) {
            long randomKey = keys.get(random.nextInt(keys.size()));
            System.out.println("i " + i + "  " + "key" + " " + randomKey + "  " + "value" + ver.get(randomKey));
            arr[i] = randomKey;


        }

        return (arr);

    }

    public long getRandomVertexId() {
        Random random = new Random();
        List<Long> keys = new ArrayList<Long>(ver.keySet());
        long randomKey = keys.get(random.nextInt(keys.size()));
        return randomKey;
    }

    public void converttoJson(long arr[]) throws Exception {
   

        List<DataObject> objList = new ArrayList<DataObject>();
        
   
   
        for (int i = 0; i < 50; i++) {
            objList.add(new DataObject( arr[i], ver.get(arr[i])) );
            
        }

        String json = new Gson().toJson(objList);

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("Output.json"));
            writer.write(json);

        } catch (IOException e) {
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
            }
        }



    }
    
    public void converttoJSON(long arr[])throws Exception
    {
    
  List<DataObject> obj = new ArrayList<DataObject>();
        
JSONObject feature = new JSONObject();
JSONArray coor = new JSONArray();

JSONObject geometry = new JSONObject();
geometry.put("type", "Polygon");
for (int i = 0; i < 50; i++) {
            coor.add(ver.get(arr[i]));
            
            }
geometry.put("coordinates", coor);
feature.put("geometry",geometry);

    
      FileWriter file = new FileWriter("output.geoJSON");
      
      try {
			file.write(feature.toJSONString());
			System.out.println("Successfully Copied JSON Object to File...");}
			catch(IOException e){e.printStackTrace();}
			finally {file.flush();
			file.close();}
    }
  
    
   


    private static class DataObject {
        private long key;
        private Collection<Double> coordinates;

        public DataObject(long key, Collection<Double> coordinates) {
            this.key = key;
            this.coordinates = coordinates;
            
        }
    }
    
   
}
