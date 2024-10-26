package com.enterprise.common.utils;

import com.enterprise.common.models.Graph;
import com.enterprise.common.models.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class MapLoader {
    public static Graph loadMap(String filePath) {
        
        Graph graph = new Graph();
        Map<String, Node> nodeMap = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(filePath));

            NodeList pointList = document.getElementsByTagName("PointInfo");
            for (int i = 0; i < pointList.getLength(); i++) {
                Element element = (Element) pointList.item(i);
                String id = element.getElementsByTagName("id").item(0).getTextContent();
                double xpos = Double.parseDouble(element.getElementsByTagName("xpos").item(0).getTextContent());
                double ypos = Double.parseDouble(element.getElementsByTagName("ypos").item(0).getTextContent());

                Node node = new Node(xpos, ypos, id);
                graph.addNode(node);
                nodeMap.put(id, node); // 使用 id 作为 key
            }

            // Second pass to create edges based on neighbor information
            for (int i = 0; i < pointList.getLength(); i++) {
                Element element = (Element) pointList.item(i);
                Node currentNode = nodeMap.get(element.getElementsByTagName("id").item(0).getTextContent());

                NodeList neighborList = element.getElementsByTagName("NeighbInfo");
                for (int j = 0; j < neighborList.getLength(); j++) {
                    Element neighborElement = (Element) neighborList.item(j);
                    int neighborId = Integer.parseInt(neighborElement.getElementsByTagName("id").item(0).getTextContent());
                    double distance = Double.parseDouble(neighborElement.getElementsByTagName("distance").item(0).getTextContent());

                    Node neighborNode = nodeMap.get(String.valueOf(neighborId));
                    if (neighborNode != null) {
                        graph.addEdge(currentNode, neighborNode, distance, 1.0); // 根据需求设置权重
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return graph;
    }

    public static Graph loadMap(File file) {
        Graph graph = new Graph();
        Map<String, Node> nodeMap = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);

            NodeList pointList = document.getElementsByTagName("PointInfo");
            for (int i = 0; i < pointList.getLength(); i++) {
                Element element = (Element) pointList.item(i);
                String id = element.getElementsByTagName("id").item(0).getTextContent();
                double xpos = Double.parseDouble(element.getElementsByTagName("xpos").item(0).getTextContent());
                double ypos = Double.parseDouble(element.getElementsByTagName("ypos").item(0).getTextContent());

                Node node = new Node(xpos, ypos, id);
                graph.addNode(node);
                nodeMap.put(id, node); // 使用 id 作为 key
            }

            // Second pass to create edges based on neighbor information
            for (int i = 0; i < pointList.getLength(); i++) {
                Element element = (Element) pointList.item(i);
                Node currentNode = nodeMap.get(element.getElementsByTagName("id").item(0).getTextContent());

                NodeList neighborList = element.getElementsByTagName("NeighbInfo");
                for (int j = 0; j < neighborList.getLength(); j++) {
                    Element neighborElement = (Element) neighborList.item(j);
                    int neighborId = Integer.parseInt(neighborElement.getElementsByTagName("id").item(0).getTextContent());
                    double distance = Double.parseDouble(neighborElement.getElementsByTagName("distance").item(0).getTextContent());

                    Node neighborNode = nodeMap.get(String.valueOf(neighborId));
                    if (neighborNode != null) {
                        graph.addEdge(currentNode, neighborNode, distance, 1.0); // 根据需求设置权重
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return graph;
    }
}
