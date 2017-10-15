package ru.vbaykov.magnit;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    //Данные для подключения к БД
    String url;
    String login;
    String pass;
    //Число N
    int n;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public void transform(String dataXML, String inputXSL, String outputXSLT) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        StreamSource xslStream = new StreamSource(inputXSL);
        Transformer transformer = factory.newTransformer(xslStream);
        StreamSource in = new StreamSource(dataXML);
        StreamResult out = new StreamResult(outputXSLT);
        transformer.transform(in, out);
    }
    public List parsingXML(String dataXML)
    {
        ArrayList<Integer> list = new ArrayList<>();
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(new File(dataXML));

            NodeList children = document.getElementsByTagName("entry");
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                NamedNodeMap attributes = node.getAttributes();
                Node attribut = attributes.getNamedItem("field");
                String valueAttribut = attribut.getNodeValue();
                list.add(Integer.parseInt(valueAttribut));
            }


        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void main(String[] args) {
        // write your code here
        ArrayList<Integer> list = new ArrayList<>();
        Main main = new Main();
        main.setN(3);
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))){
//            System.out.println("Введите адрес базы");
//            main.setUrl(reader.readLine());
//            System.out.println("Login");
//            main.setLogin(reader.readLine());
//            System.out.println("Password");
//            main.setPass(reader.readLine());
//            System.out.println("N число");
//            main.setN(Integer.parseInt(String.valueOf(Integer.parseInt(reader.readLine()))));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/TEST", "pgadmin", "test");
             Statement statement = connection.createStatement()) {
            //Чистим таблицу если не пустая
            //System.out.println("Opened database successfully");
            //if (statement.getResultSet().next())
            statement.executeUpdate("DELETE from TEST");
            //statement.executeUpdate("CREATE TABLE TEST (FIELD INT)");
            //Вставка данных
            for (int i = 1; i < main.getN() + 1; i++) {
                if (i < 1000000)
                    statement.executeUpdate("INSERT INTO TEST (FIELD) VALUES (" + i + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/TEST", "pgadmin", "test");
             Statement statement = connection.createStatement()) {

            //Получение и сохранение в List
            ResultSet resultSet = statement.executeQuery("SELECT * FROM test");
            while (resultSet.next()) {
                list.add(resultSet.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //Сохраняем в xml
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();

            Document document = builder.newDocument();
            Element rootElement = document.createElement("entries");
            document.appendChild(rootElement);

            for (int i = 0; i < list.size(); i++) {
                Element entryElement = document.createElement("entry");
                rootElement.appendChild(entryElement);

                Element fieldElement = document.createElement("field");
                fieldElement.appendChild(document.createTextNode(String.valueOf(list.get(i))));
                entryElement.appendChild(fieldElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(document);
            StreamResult result1 = new StreamResult(new File("1.xml"));
            transformer.transform(source, result1);

            main.transform("1.xml", "src/styleXSLT.xsl", "2.xml");
            ArrayList<Integer> list1 = (ArrayList<Integer>) main.parsingXML("2.xml");
            int result = 0;
            for (int i = 0; i < list1.size(); i++) {
                result += list1.get(i);
            }
            System.out.println(result);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }


    }
}
