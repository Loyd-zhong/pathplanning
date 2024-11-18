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
            for (int i = 0; i < pointList.getLength(); i++) {
                Element element = (Element) pointList.item(i);
                Node currentNode = nodeMap.get(element.getElementsByTagName("id").item(0).getTextContent());
                
                NodeList neighborInfos = element.getElementsByTagName("NeighbInfo");
                for (int j = 0; j < neighborInfos.getLength(); j++) {
                    try {
                        Element neighborInfo = (Element) neighborInfos.item(j);
                        NodeList idElements = neighborInfo.getElementsByTagName("id");
                        for (int k = 0; k < idElements.getLength(); k++) {
                            Element idElement = (Element) idElements.item(k);
                            String neighborId = idElement.getTextContent();
                            
                            // 检查是否存在isLine属性
                            boolean isLine = idElement.hasAttribute("isLine") ? 
                                    "0".equals(idElement.getAttribute("isLine")) : true;
                                    
                            Node neighborNode = nodeMap.get(neighborId);
                            if (neighborNode != null) {
                                double distance = Double.parseDouble(
                                    neighborInfo.getElementsByTagName("distance").item(k).getTextContent()
                                );
                                
                                // 检查是否有控制点
                                NodeList ctrlPoints = neighborInfo.getElementsByTagName("CtrlPoint");
                                boolean hasCurve = ctrlPoints.getLength() > 0;
                                
                                // 无论是否存在边都添加，确保双向连接
                                graph.addEdge(currentNode, neighborNode, distance, 1.0);
                                System.out.println("Adding edge: " + currentNode.getId() + " -> " + neighborId);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing edge: " + e.getMessage());
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
