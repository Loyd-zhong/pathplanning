package com.enterprise.common.utils;

import com.enterprise.common.models.Graph;
import com.enterprise.common.models.Node;
import com.enterprise.common.models.Edge;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapLoader {
    public static Graph loadMap(String filePath) {
        Graph graph = new Graph();
        Map<String, Node> nodeMap = new HashMap<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(filePath));
            
            // 第一遍：创建所有节点
            NodeList pointList = document.getElementsByTagName("PointInfo");
            for (int i = 0; i < pointList.getLength(); i++) {
                Element element = (Element) pointList.item(i);
                String id = element.getElementsByTagName("id").item(0).getTextContent();
                double xpos = Double.parseDouble(element.getElementsByTagName("xpos").item(0).getTextContent());
                double ypos = Double.parseDouble(element.getElementsByTagName("ypos").item(0).getTextContent());
                
                Node node = new Node(xpos, ypos, id);
                graph.addNode(node);
                nodeMap.put(id, node);
                //System.out.println("Added node: " + id + " at (" + xpos + "," + ypos + ")");
            }
            
            // 第二遍：处理所有边的连接
            Map<String, Set<String>> connections = new HashMap<>();
            
            // 首先收集所有的边连接信息
            for (int i = 0; i < pointList.getLength(); i++) {
                Element element = (Element) pointList.item(i);
                String fromId = element.getElementsByTagName("id").item(0).getTextContent();
                
                NodeList neighborInfos = element.getElementsByTagName("NeighbInfo");
                for (int j = 0; j < neighborInfos.getLength(); j++) {
                    Element neighborInfo = (Element) neighborInfos.item(j);
                    NodeList idElements = neighborInfo.getElementsByTagName("id");
                    
                    for (int k = 0; k < idElements.getLength(); k++) {
                        String toId = idElements.item(k).getTextContent();
                        NodeList reverElements = neighborInfo.getElementsByTagName("Rever");
                        boolean isDirectional = reverElements.getLength() > 0 && 
                                             (reverElements.item(0).getTextContent().isEmpty() || "0".equals(reverElements.item(0).getTextContent()));
                        
                        double distance = Double.parseDouble(
                            neighborInfo.getElementsByTagName("distance").item(k).getTextContent()
                        );
                        
                        NodeList ctrlPoints = neighborInfo.getElementsByTagName("CtrlPoint");
                        boolean hasCurve = ctrlPoints.getLength() > 0;
                        
                        Node fromNode = nodeMap.get(fromId);
                        Node toNode = nodeMap.get(toId);
                        
                        if (fromNode != null && toNode != null) {
                            Edge edge;
                            if (hasCurve) {
                                edge = graph.addCurvedEdge(fromNode, toNode, isDirectional);
                            } else {
                                edge = graph.addEdge(fromNode, toNode, isDirectional, distance, 1.0);
                            }
                            
                            NodeList speedElements = neighborInfo.getElementsByTagName("emptyVehicleSpeed");
                            if (speedElements.getLength() > 0) {
                                String speedText = speedElements.item(0).getTextContent();
                                if (!speedText.isEmpty()) {
                                    edge.emptyVehicleSpeed = Double.parseDouble(speedText);
                                }
                            }

                            speedElements = neighborInfo.getElementsByTagName("backEmptyShelfSpeed");
                            if (speedElements.getLength() > 0) {
                                String speedText = speedElements.item(0).getTextContent();
                                if (!speedText.isEmpty()) {
                                    edge.backEmptyShelfSpeed = Double.parseDouble(speedText);
                                }
                            }

                            speedElements = neighborInfo.getElementsByTagName("backToBackRackSpeed");
                            if (speedElements.getLength() > 0) {
                                String speedText = speedElements.item(0).getTextContent();
                                if (!speedText.isEmpty()) {
                                    edge.backToBackRackSpeed = Double.parseDouble(speedText);
                                }
                            }
                            speedElements = neighborInfo.getElementsByTagName("backfillShelfSpeed");
                            if (speedElements.getLength() > 0) {
                                String speedText = speedElements.item(0).getTextContent();
                                if (!speedText.isEmpty()) {
                                    edge.backfillShelfSpeed = Double.parseDouble(speedText);
                                }
                            }
                        }
                    }
                }
            }
            
            return graph;
        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load map", e);
        }
    }
}
